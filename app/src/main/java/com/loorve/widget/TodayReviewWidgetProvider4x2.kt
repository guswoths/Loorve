package com.loorve.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.loorve.MainActivity
import com.loorve.R
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class TodayReviewWidgetProvider4x2 : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        TodayReviewWidgetManager.updateAllWidgets(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            TodayReviewWidgetManager.ACTION_ITEM_CLICK -> {
                val clickType = intent.getStringExtra(TodayReviewWidgetManager.EXTRA_CLICK_TYPE)
                val scheduleId = intent.getStringExtra(TodayReviewWidgetManager.EXTRA_SCHEDULE_ID).orEmpty()
                if (clickType == TodayReviewWidgetManager.CLICK_TYPE_TOGGLE) {
                    val isCompleted = intent.getBooleanExtra(TodayReviewWidgetManager.EXTRA_IS_COMPLETED, false)
                    TodayReviewWidgetManager.handleToggleCheck(context, scheduleId, isCompleted)
                } else if (clickType == TodayReviewWidgetManager.CLICK_TYPE_OPEN_APP) {
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(openIntent)
                }
            }
            TodayReviewWidgetManager.ACTION_REFRESH -> {
                TodayReviewWidgetManager.updateAllWidgets(context)
            }
        }
    }

    companion object {
        private val seoulZone = ZoneId.of("Asia/Seoul")
        private val dateFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_today_review_4x2)

            // Responsive layout: check width for 2x2 vs 4x2
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
            val isCompact = minWidth in 1..210

            val today = LocalDate.now(seoulZone)
            if (isCompact) {
                // 2x2: keep date text fixed at upper right with compact formatting
                val compactFormatter = DateTimeFormatter.ofPattern("M.d (E)", Locale.KOREAN)
                views.setTextViewText(R.id.widget_date_text, today.format(compactFormatter))
            } else {
                // 4x2: full Korean date formatting
                views.setTextViewText(R.id.widget_date_text, today.format(dateFormatter))
            }
            views.setViewVisibility(R.id.widget_date_text, View.VISIBLE)
            views.setViewVisibility(R.id.widget_dot_separator, View.VISIBLE)

            // Progress text & progress bar
            val schedules = TodayReviewWidgetManager.cachedSchedules
            val totalCount = schedules.size
            val completedCount = schedules.count { it.isCompleted }
            val progress = if (totalCount > 0) (completedCount * 100) / totalCount else 0

            views.setTextViewText(R.id.widget_progress_text, "${completedCount}/${totalCount} 완료")
            views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)

            // Header click -> Open app
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header, appPendingIntent)

            // ListView RemoteViewsService adapter
            val serviceIntent = Intent(context, TodayReviewWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
            views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_text)

            // PendingIntent template for item clicks (checkbox toggle or row click)
            val itemClickIntent = Intent(context, TodayReviewWidgetProvider4x2::class.java).apply {
                action = TodayReviewWidgetManager.ACTION_ITEM_CLICK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val itemClickPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                itemClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list_view, itemClickPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
