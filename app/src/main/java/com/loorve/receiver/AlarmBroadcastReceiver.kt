package com.loorve.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.loorve.MainActivity
import com.loorve.R
import com.loorve.data.notification.EXTRA_REVIEW_SCHEDULE_ID

private const val CHANNEL_ID = "review_alarm_channel"
private const val CHANNEL_NAME = "복습 알림"
private const val CHANNEL_DESC = "복습 예정일에 알려드립니다."
private const val TAG = "AlarmBroadcastReceiver"

class AlarmBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getStringExtra(EXTRA_REVIEW_SCHEDULE_ID) ?: run {
            Log.e(TAG, "EXTRA_REVIEW_SCHEDULE_ID가 없습니다.")
            return
        }
        Log.d(TAG, "복습 알람 수신: scheduleId=$scheduleId")

        ensureNotificationChannel(context)
        showNotification(context, scheduleId)
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, scheduleId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                Log.w(TAG, "POST_NOTIFICATIONS 권한 없음. 알림 표시를 건너뜁니다. scheduleId=$scheduleId")
                return
            }
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REVIEW_SCHEDULE_ID, scheduleId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            scheduleId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("오늘의 복습 일정이 있습니다 📚")
            .setContentText("지금 앱을 열어 복습을 완료해 보세요!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(scheduleId.hashCode(), notification)
    }
}