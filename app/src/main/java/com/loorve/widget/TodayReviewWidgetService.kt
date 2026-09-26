package com.loorve.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.loorve.R

class TodayReviewWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodayReviewRemoteViewsFactory(applicationContext)
    }
}

class TodayReviewRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<TodayWidgetScheduleItem> = emptyList()

    override fun onCreate() {
        items = TodayReviewWidgetManager.cachedSchedules
    }

    override fun onDataSetChanged() {
        // Fetch or use cached schedules
        items = if (TodayReviewWidgetManager.isLoaded) {
            TodayReviewWidgetManager.cachedSchedules
        } else {
            kotlinx.coroutines.runBlocking {
                val loaded = TodayReviewWidgetManager.loadTodaySchedules(context)
                TodayReviewWidgetManager.setInitialCachedSchedules(loaded)
                loaded
            }
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position !in items.indices) return null
        val item = items[position]

        val views = RemoteViews(context.packageName, R.layout.widget_today_review_item)
        views.setTextViewText(R.id.widget_item_title, item.title)
        views.setTextViewText(R.id.widget_item_order, "${item.reviewOrder}회차")

        if (item.isCompleted) {
            views.setImageViewResource(R.id.widget_item_check, R.drawable.widget_checkbox_checked)
            views.setTextColor(R.id.widget_item_title, Color.parseColor("#94A3B8"))
        } else {
            views.setImageViewResource(R.id.widget_item_check, R.drawable.widget_checkbox_unchecked)
            views.setTextColor(R.id.widget_item_title, Color.parseColor("#0F172A"))
        }

        // Checkbox click fill-in intent
        val checkFillInIntent = Intent().apply {
            putExtra(TodayReviewWidgetManager.EXTRA_CLICK_TYPE, TodayReviewWidgetManager.CLICK_TYPE_TOGGLE)
            putExtra(TodayReviewWidgetManager.EXTRA_SCHEDULE_ID, item.id)
            putExtra(TodayReviewWidgetManager.EXTRA_IS_COMPLETED, item.isCompleted)
        }
        views.setOnClickFillInIntent(R.id.widget_item_check, checkFillInIntent)

        // Row click fill-in intent to open app
        val rowFillInIntent = Intent().apply {
            putExtra(TodayReviewWidgetManager.EXTRA_CLICK_TYPE, TodayReviewWidgetManager.CLICK_TYPE_OPEN_APP)
            putExtra(TodayReviewWidgetManager.EXTRA_SCHEDULE_ID, item.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_row, rowFillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position in items.indices) {
            items[position].id.hashCode().toLong()
        } else {
            position.toLong()
        }
    }

    override fun hasStableIds(): Boolean = true
}
