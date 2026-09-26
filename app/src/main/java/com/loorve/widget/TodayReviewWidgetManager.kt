package com.loorve.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.loorve.R
import com.loorve.data.notification.ReviewAlarmScheduler
import com.loorve.domain.model.ReviewSchedule
import com.loorve.domain.model.ReviewStatus
import com.loorve.domain.repository.ReviewBlockRepository
import com.loorve.domain.repository.ReviewScheduleItemRepository
import com.loorve.domain.repository.ReviewScheduleRepository
import com.loorve.util.CalendarRefreshBus
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun reviewScheduleItemRepository(): ReviewScheduleItemRepository
    fun reviewScheduleRepository(): ReviewScheduleRepository
    fun reviewBlockRepository(): ReviewBlockRepository
    fun calendarRefreshBus(): CalendarRefreshBus
    fun reviewAlarmScheduler(): ReviewAlarmScheduler
}

data class TodayWidgetScheduleItem(
    val id: String,
    val title: String,
    val reviewOrder: Int,
    val isCompleted: Boolean,
    val reviewDate: Long,
    val isOverdue: Boolean
)

object TodayReviewWidgetManager {

    const val ACTION_ITEM_CLICK = "com.loorve.widget.ACTION_ITEM_CLICK"
    const val ACTION_REFRESH = "com.loorve.widget.ACTION_REFRESH"
    const val EXTRA_CLICK_TYPE = "extra_click_type"
    const val CLICK_TYPE_TOGGLE = "click_type_toggle"
    const val CLICK_TYPE_OPEN_APP = "click_type_open_app"

    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_IS_COMPLETED = "extra_is_completed"

    private val seoulZone = ZoneId.of("Asia/Seoul")

    @Volatile
    var isLoaded: Boolean = false
        private set

    @Volatile
    var cachedSchedules: List<TodayWidgetScheduleItem> = emptyList()
        private set

    fun setInitialCachedSchedules(schedules: List<TodayWidgetScheduleItem>) {
        cachedSchedules = schedules
        isLoaded = true
    }

    fun updateAllWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val freshSchedules = loadTodaySchedules(context)

            // Keep optimistic completion state from memory cache so an item never reverts or disappears
            val cachedMap = cachedSchedules.associateBy { it.id }
            val cachedTitleOrderMap = cachedSchedules.associateBy { "${it.title}_${it.reviewOrder}" }

            val mergedSchedules = freshSchedules.map { fresh ->
                val cached = cachedMap[fresh.id] ?: cachedTitleOrderMap["${fresh.title}_${fresh.reviewOrder}"]
                if (cached != null && cached.isCompleted != fresh.isCompleted) {
                    fresh.copy(isCompleted = cached.isCompleted)
                } else {
                    fresh
                }
            }

            // Ensure any item currently in cachedSchedules is preserved in the widget
            val missingFromFresh = cachedSchedules.filter { cached ->
                mergedSchedules.none { it.id == cached.id || (it.title == cached.title && it.reviewOrder == cached.reviewOrder) }
            }

            cachedSchedules = (mergedSchedules + missingFromFresh).sortedWith(
                compareBy<TodayWidgetScheduleItem> { it.reviewDate }
                    .thenBy { it.reviewOrder }
                    .thenBy { it.title }
            )
            isLoaded = true

            withContext(Dispatchers.Main) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val comp4x2 = ComponentName(context, TodayReviewWidgetProvider4x2::class.java)
                val ids4x2 = appWidgetManager.getAppWidgetIds(comp4x2)

                // Update widget layout (header, progress bar)
                for (id in ids4x2) {
                    TodayReviewWidgetProvider4x2.updateAppWidget(context, appWidgetManager, id)
                }

                // Notify ListView data changed
                appWidgetManager.notifyAppWidgetViewDataChanged(ids4x2, R.id.widget_list_view)
            }
        }
    }

    suspend fun loadTodaySchedules(context: Context): List<TodayWidgetScheduleItem> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext emptyList()
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val today = LocalDate.now(seoulZone)

        // 1. Fetch ReviewScheduleItem list
        val itemsResult = entryPoint.reviewScheduleItemRepository().getAllScheduleItems(uid)
        val items = itemsResult.getOrDefault(emptyList())

        // 2. Fetch legacy schedules (if any)
        val dateStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val legacyResult: List<ReviewSchedule> = runCatching {
            entryPoint.reviewScheduleRepository().getReviewSchedulesByDateRange(uid, dateStr, dateStr).first()
        }.getOrDefault(emptyList())

        val todayItems = items.filter { item ->
            val itemDate = Instant.ofEpochMilli(item.reviewDate).atZone(seoulZone).toLocalDate()
            val completedToday = item.completedAt != null &&
                Instant.ofEpochMilli(item.completedAt).atZone(seoulZone).toLocalDate() == today
            // Include today's items, overdue incomplete items, and items completed today
            itemDate == today || (itemDate.isBefore(today) && item.status != ReviewStatus.COMPLETED) || completedToday
        }.map { item ->
            val itemDate = Instant.ofEpochMilli(item.reviewDate).atZone(seoulZone).toLocalDate()
            TodayWidgetScheduleItem(
                id = item.id,
                title = item.title.ifBlank { "복습 일정" },
                reviewOrder = item.reviewOrder,
                isCompleted = item.status == ReviewStatus.COMPLETED,
                reviewDate = item.reviewDate,
                isOverdue = itemDate.isBefore(today)
            )
        }

        val legacyItems: List<TodayWidgetScheduleItem> = legacyResult.map { legacy ->
            TodayWidgetScheduleItem(
                id = legacy.scheduleId,
                title = legacy.title.ifBlank { "복습 일정" },
                reviewOrder = legacy.reviewOrder,
                isCompleted = legacy.isCompleted,
                reviewDate = legacy.reviewDate,
                isOverdue = false
            )
        }

        // Deduplicate and merge modern + legacy items:
        // Group by title and reviewOrder so same schedule from both collections is merged cleanly
        (todayItems + legacyItems)
            .groupBy { "${it.title}_${it.reviewOrder}" }
            .values
            .map { group ->
                val isAnyCompleted = group.any { it.isCompleted }
                val primary = group.first()
                primary.copy(isCompleted = isAnyCompleted)
            }
            .sortedWith(
                // Do NOT sort by isCompleted! Sort only by reviewDate and reviewOrder so checked items stay in place
                compareBy<TodayWidgetScheduleItem> { it.reviewDate }
                    .thenBy { it.reviewOrder }
                    .thenBy { it.title }
            )
    }

    fun handleToggleCheck(context: Context, scheduleId: String, currentCompleted: Boolean) {
        if (scheduleId.isBlank()) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val newCompleted = !currentCompleted

        // 1. Optimistically update memory cache immediately so UI feels instantaneous!
        // The item stays at the exact same index with isCompleted toggled.
        cachedSchedules = cachedSchedules.map {
            if (it.id == scheduleId) it.copy(isCompleted = newCompleted) else it
        }
        isLoaded = true

        // 2. Notify widget immediately
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val comp4x2 = ComponentName(context, TodayReviewWidgetProvider4x2::class.java)
        val ids4x2 = appWidgetManager.getAppWidgetIds(comp4x2)
        for (id in ids4x2) {
            TodayReviewWidgetProvider4x2.updateAppWidget(context, appWidgetManager, id)
        }
        appWidgetManager.notifyAppWidgetViewDataChanged(ids4x2, R.id.widget_list_view)

        // 3. Asynchronously update Firestore and refresh app
        CoroutineScope(Dispatchers.IO).launch {
            val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)

            // Both repositories will synchronize across reviewScheduleItems & reviewSchedules
            val itemResult = entryPoint.reviewScheduleItemRepository().updateScheduleCompletion(uid, scheduleId, newCompleted)
            if (itemResult.isFailure) {
                runCatching {
                    entryPoint.reviewScheduleRepository().updateReviewCompletion(uid, scheduleId, newCompleted)
                }
            }

            if (newCompleted) {
                runCatching {
                    entryPoint.reviewAlarmScheduler().cancelReviewAlarm(scheduleId)
                }
            }

            // Notify app screens (HomeScreen, ReviewCalendarScreen, etc.)
            entryPoint.calendarRefreshBus().notifyRefresh()

            // Refresh widgets with latest data
            updateAllWidgets(context)
        }
    }
}
