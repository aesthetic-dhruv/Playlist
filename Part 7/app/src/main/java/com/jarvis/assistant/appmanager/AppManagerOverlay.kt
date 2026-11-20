package com.jarvis.assistant.appmanager

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView

/**
 * Misri Phone Cleaner — SD Maid jaisi overlay screen (Run #9).
 *
 * Queue chalte waqt Settings ka App info page samne hota hai, isliye
 * Activity ke andar ka progress card dikhta hi nahi. Ye overlay
 * WindowManager se Settings ke UPAR tairta hai: beech me Misri ka
 * avatar (app icon — user ka Misri logo), neeche progress card
 * (kaunsi app, kitna hua, Cancel button). Screenshot wali SD Maid
 * screen jaisa, bas Android avatar ki jagah Misri ka avatar.
 *
 * Permission: SYSTEM_ALERT_WINDOW (manifest me pehle se hai). Agar user
 * ne "Display over other apps" allow nahi kiya to overlay chupchaap
 * nahi dikhta — kaam phir bhi notification + Activity card se chalta
 * hai. Overlay Accessibility clicks ko nahi rokta (wo touch se nahi,
 * AccessibilityService se hote hain); sirf neeche wali patti ke
 * touches overlay khud leta hai (Cancel button ke liye).
 */
object AppManagerOverlay {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var lastAction: CleanerAction = CleanerAction.CLEAR_CACHE

    fun isShowing(): Boolean = overlayView != null

    /** Overlay dikhao. False = overlay permission nahi mili, overlay ke bina chalega. */
    fun show(context: Context, action: CleanerAction): Boolean {
        if (overlayView != null) return true
        val ctx = context.applicationContext
        if (!Settings.canDrawOverlays(ctx)) return false
        lastAction = action
        return try {
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val view = LayoutInflater.from(ctx).inflate(
                com.jarvis.assistant.R.layout.layout_app_manager_overlay, null
            )
            view.findViewById<Button>(com.jarvis.assistant.R.id.btnOverlayCancel)
                .setOnClickListener {
                    AppManagerDriver.stop()
                    hide()
                }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.BOTTOM }
            wm.addView(view, params)
            windowManager = wm
            overlayView = view
            true
        } catch (e: Exception) {
            android.util.Log.w("MisriAppManager", "overlay show failed: ${e.message}")
            false
        }
    }

    /** Progress update karo (driver ke publish() se call hota hai). */
    fun update(progress: AppManagerDriver.Progress) {
        val view = overlayView ?: return
        try {
            val title = when (progress.action) {
                CleanerAction.FORCE_STOP -> "Misri force stop kar rahi hai…"
                CleanerAction.CLEAR_CACHE -> "Misri cache clear kar rahi hai…"
                CleanerAction.CLEAR_DATA -> "Misri data clear kar rahi hai…"
            }
            view.findViewById<TextView>(com.jarvis.assistant.R.id.tvOverlayTitle).text = title
            view.findViewById<TextView>(com.jarvis.assistant.R.id.tvOverlayApp).text =
                if (progress.running) "Abhi: ${progress.currentLabel}" else "Khatam"
            val doneCount = progress.done + progress.failed + progress.skipped
            val total = progress.total.coerceAtLeast(1)
            val bar = view.findViewById<ProgressBar>(com.jarvis.assistant.R.id.overlayProgressBar)
            bar.max = total
            bar.progress = doneCount
            view.findViewById<TextView>(com.jarvis.assistant.R.id.tvOverlayPercent).text =
                "${doneCount * 100 / total}%"
            view.findViewById<TextView>(com.jarvis.assistant.R.id.tvOverlayCounts).text =
                "Done: ${progress.done} • Fail: ${progress.failed} • Skip: ${progress.skipped} • Total: ${progress.total}"
            view.findViewById<ImageView>(com.jarvis.assistant.R.id.ivOverlayAvatar)
                .setImageResource(com.jarvis.assistant.R.mipmap.ic_launcher_round)
        } catch (e: Exception) {
            android.util.Log.w("MisriAppManager", "overlay update failed: ${e.message}")
        }
    }

    fun hide() {
        val view = overlayView ?: return
        try {
            windowManager?.removeView(view)
        } catch (e: Exception) {
            // Pehle hi hata diya gaya ho to ignore karo.
        }
        overlayView = null
        windowManager = null
    }
}
