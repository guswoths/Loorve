package com.loorve.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.loorve.util.showReviewNotification

class LoorveFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: 서버에 새 FCM 토큰 전송 로직 구현
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: getString(com.loorve.R.string.app_name)
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: return

        showReviewNotification(
            context = this,
            notificationId = remoteMessage.messageId ?: "fcm_${System.currentTimeMillis()}",
            title = title,
            text = body
        )
    }
}
