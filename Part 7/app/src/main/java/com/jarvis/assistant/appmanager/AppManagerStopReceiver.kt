package com.jarvis.assistant.appmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Notification ke Stop button se aata hai — queue turant rok deta hai.
 * Manifest me declared hai (patch-app-manager.py dekho).
 */
class AppManagerStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == AppManagerNotifier.ACTION_STOP_QUEUE) {
            Log.i("MisriAppManager", "queue stopped from notification")
            AppManagerDriver.stop()
            AppManagerNotifier.cancel(context)
        }
    }
}
