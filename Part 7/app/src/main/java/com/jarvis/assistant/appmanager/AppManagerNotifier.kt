package com.jarvis.assistant.appmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Queue chalte waqt progress notification — kyunki queue ke dauran
 * Settings app samne hoti hai, Misri ki screen nahi dikhti.
 * Notification me current app, done/failed/skipped aur Stop action hota hai.
 * Android 13+ par POST_NOTIFICATIONS runtime permission chahiye; na mile
 * to notify() SecurityException dega — use pakad kar ignore karte hain,
 * queue phir bhi chalti hai aur final log Misri khulne par dikhta hai.
 */
object AppManagerNotifier {

    private const val CHANNEL_ID = "misri_app_manager"
    private const val NOTIFICATION_ID = 4201
    const val ACTION_STOP_QUEUE = "com.jarvis.assistant.appmanager.STOP_QUEUE"

    private fun channel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Phone Cleaner", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "Misri Phone Cleaner queue progress"
                    }
                )
            }
        }
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, AppManagerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun stopIntent(context: Context): PendingIntent {
        val intent = Intent(context, AppManagerStopReceiver::class.java).apply {
            action = ACTION_STOP_QUEUE
        }
        return PendingIntent.getBroadcast(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showProgress(context: Context, progress: AppManagerDriver.Progress) {
        channel(context)
        val text = if (progress.running) {
            "Kar rahi hoon: ${progress.currentLabel} — ${progress.done} done, ${progress.failed} fail, ${progress.skipped} skip"
        } else {
            "Queue khatam: ${progress.done} done, ${progress.failed} fail, ${progress.skipped} skip"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Misri Phone Cleaner — ek-ek karke ho raha hai")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent(context))
            .setOngoing(progress.running)
            .setOnlyAlertOnce(true)
            .apply {
                if (progress.running) {
                    addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent(context))
                    setProgress(progress.total, progress.done + progress.failed + progress.skipped, false)
                }
            }
            .build()
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission nahi hai — queue phir bhi chalegi.
        }
    }

    fun showFinished(context: Context, progress: AppManagerDriver.Progress) {
        showProgress(context, progress)
        // Finished notification ongoing nahi hai, user swipe karke hata sakta hai.
    }

    fun cancel(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {
        }
    }
}
