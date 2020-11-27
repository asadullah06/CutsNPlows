package Com.app.cuts.plows.ui

import Com.app.cuts.plows.R
import Com.app.cuts.plows.utils.UPDATE_UI_AGAINST_NOTIFICATION
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCMPayload", remoteMessage.data.toString())
        Log.d("FCMPayload", remoteMessage.notification.toString())

        Log.d("FCMPayload", remoteMessage.data["alert"].toString())
        sendBroadcast(Intent(UPDATE_UI_AGAINST_NOTIFICATION))
        sendNotification(remoteMessage.data["alert"].toString())
    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
    }

    private fun sendNotification(messageBody: String = "") {
        val intent = Intent(this, SplashScreenActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.app_icon_small)
            .setContentTitle(getString(R.string.app_name))
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_MAX)    // to show notification Heads-up Notifications
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(this)

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }
}