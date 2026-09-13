package com.loorve.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.loorve.data.notification.EXTRA_REVIEW_SCHEDULE_ID
import com.loorve.util.showReviewNotification

private const val TAG = "AlarmBroadcastReceiver"

class AlarmBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getStringExtra(EXTRA_REVIEW_SCHEDULE_ID) ?: run {
            Log.e(TAG, "EXTRA_REVIEW_SCHEDULE_ID가 없습니다.")
            return
        }
        Log.d(TAG, "복습 알람 수신: scheduleId=$scheduleId")

        showReviewNotification(
            context = context,
            notificationId = scheduleId,
            title = "오늘의 복습 일정이 있습니다 📚",
            text = "지금 앱을 열어 복습을 완료해 보세요!"
        )
    }
}