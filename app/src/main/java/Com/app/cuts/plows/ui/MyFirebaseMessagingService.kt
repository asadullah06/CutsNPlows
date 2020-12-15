package Com.app.cuts.plows.ui

import Com.app.cuts.plows.R
import Com.app.cuts.plows.ui.Chat.MessageThreadActivity
import Com.app.cuts.plows.utils.CommonMethods
import Com.app.cuts.plows.utils.UPDATE_MESSAGES
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
import org.json.JSONObject


class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCMPayload", remoteMessage.data.toString())
        Log.d("FCMPayload", remoteMessage.notification.toString())

        Log.d("FCMPayload", remoteMessage.data["alert"].toString())
        Log.d("FCMPayload", "\n\n API  number =: ${remoteMessage.data["apinumber"].toString()}")
        var intent: Intent? = null
        if (remoteMessage.data["apinumber"]?.toInt() == 29) {
            val extraObject = JSONObject(remoteMessage.data["extra"] as String)
            val bookingObject = extraObject.getJSONObject("booking")
            intent = Intent(this, MessageThreadActivity::class.java)
            intent.putExtra("receiver_id", bookingObject.getString("fld_userid"))
            intent.putExtra(
                "user_name",
                "${bookingObject.getString("fld_fname")} ${bookingObject.getString("fld_lname")}"
            )
            if (bookingObject.getInt("fld_role") == 1) {
                intent.putExtra("user_role", resources.getString(R.string.customer))
            } else if (bookingObject.getInt("fld_role") == 2) {
                intent.putExtra("user_role", resources.getString(R.string.service_provider))
            }
            intent.putExtra("user_image", bookingObject.getString("fld_profile_pic"))
            sendBroadcast(Intent(UPDATE_MESSAGES))
        } else if (remoteMessage.data["apinumber"]?.toInt() == 12 || remoteMessage.data["apinumber"]?.toInt() == 18 || remoteMessage.data["apinumber"]?.toInt() == 16 || remoteMessage.data["apinumber"]?.toInt() == 20 || remoteMessage.data["apinumber"]?.toInt() == 21) {
            sendBroadcast(Intent(UPDATE_UI_AGAINST_NOTIFICATION))
        }
        if (CommonMethods.isActivityVisible())
            return
        if (intent == null) {
            intent = Intent(this, SplashScreenActivity::class.java)
        }
        sendNotification(
            remoteMessage.data["alert"].toString(),
            intent
        )
    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
    }

    private fun sendNotification(messageBody: String = "", intent:Intent?) {
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
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