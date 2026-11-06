package com.jarvis.assistant.appmanager

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Misri App Manager — Accessibility automation driver.
 *
 * REUSE, not a second service: ye object existing
 * JarvisAccessibilityService ke andar se chalta hai. Service ke
 * onAccessibilityEvent() me sirf ek line judi hai jo yahan forward karti
 * hai. Service khud declare to hai (Run #6 APK me aapt2 se verify), par
 * ENABLED hona alag baat hai — user ko Settings > Accessibility me Misri
 * ko khud on karna padta hai, app khud se on nahi kar sakti.
 *
 * Kaam ka tareeka (Baxa / Hibernator jaisa):
 *  1. Ek tap se queue shuru hoti hai.
 *  2. Driver har app ka Settings > App info page kholta hai
 *     (ACTION_APPLICATION_DETAILS_SETTINGS).
 *  3. Screen par "Force stop" / "Storage & cache" / "Clear cache" /
 *     "Clear storage" button dhoondh kar click karta hai, confirm dialog
 *     par "OK" / "Delete" dabata hai, phir agla app kholta hai.
 *  4. Har app par 2-4 second lagte hain. Screen on rakhni padti hai.
 *
 * Safety:
 *  - Clear Data ki queue sirf Activity se double-confirmation ke baad hi
 *    banti hai. Voice se Clear Data kabhi queue nahi hoti.
 *  - Hard-excluded packages (AppManagerExclusions) queue me aa hi nahi sakte.
 *  - Har app ka 20 second timeout: button na mile to FAILED likh kar agla app.
 *  - User beech me Stop daba de to queue turant ruk jati hai.
 */
object AppManagerDriver {

    private const val TAG = "MisriAppManager"
    private const val SETTINGS_PACKAGE = "com.android.settings"
    private const val PER_APP_TIMEOUT_MS = 20_000L
    private const val STEP_RETRY_MS = 700L
    private const val NEXT_APP_DELAY_MS = 500L

    data class Job(val packageName: String, val label: String, val action: CleanerAction)

    data class Progress(
        val total: Int,
        val done: Int,
        val failed: Int,
        val skipped: Int,
        val currentLabel: String,
        val running: Boolean,
        val log: List<JobResult>
    )

    private enum class Step {
        WAIT_DETAILS, CLICK_FORCE_STOP, WAIT_FORCE_CONFIRM,
        CLICK_STORAGE_ROW, WAIT_STORAGE_SCREEN, WAIT_DELETE_CONFIRM, IDLE
    }

    private val _progress = MutableStateFlow<Progress?>(null)
    val progress: StateFlow<Progress?> = _progress.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var appContext: Context? = null
    private var jobs: List<Job> = emptyList()
    private var index: Int = -1
    private var step: Step = Step.IDLE
    private var stepEnteredAt: Long = 0L
    private var running: Boolean = false
    private val results = ArrayList<JobResult>()

    private val watchdog = object : Runnable {
        override fun run() {
            if (!running) return
            val now = System.currentTimeMillis()
            if (index in jobs.indices && now - stepEnteredAt > PER_APP_TIMEOUT_MS) {
                val job = jobs[index]
                Log.w(TAG, "timeout on ${job.packageName} at $step")
                finishCurrent(JobStatus.FAILED, "Time khatam — button nahi mila ya screen nahi khuli")
                openNext()
            } else {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    fun isRunning(): Boolean = running

    /**
     * Queue shuru karo. False = AccessibilityService enabled nahi hai.
     * Caller (Activity) ko pehle JarvisAccessibilityService.isEnabled()
     * check karke enable-flow dikhana chahiye.
     */
    fun start(context: Context, newJobs: List<Job>): Boolean {
        if (!com.jarvis.assistant.automation.accessibility.JarvisAccessibilityService.isEnabled()) {
            Log.w(TAG, "start refused: accessibility service not enabled")
            return false
        }
        val safeJobs = newJobs.filter { !AppManagerExclusions.isExcluded(it.packageName) }
        if (safeJobs.isEmpty()) return false
        stopInternal(keepProgress = false)
        appContext = context.applicationContext
        jobs = safeJobs
        index = -1
        results.clear()
        running = true
        publish(currentLabel = "")
        handler.post(watchdog)
        AppManagerNotifier.showProgress(context, currentProgress())
        openNext()
        return true
    }

    /** User ne Stop dabaya, ya queue khatam hui. */
    fun stop() {
        if (!running) return
        stopInternal(keepProgress = true)
        appContext?.let { AppManagerNotifier.cancel(it) }
        Log.i(TAG, "queue stopped by user")
    }

    private fun stopInternal(keepProgress: Boolean) {
        running = false
        handler.removeCallbacks(watchdog)
        handler.removeCallbacksAndMessages(null)
        step = Step.IDLE
        if (!keepProgress) {
            _progress.value = null
        }
    }

    /**
     * JarvisAccessibilityService.onAccessibilityEvent() se call hota hai.
     * Idle ho to kuch nahi karta (false) — baaki service features
     * (WhatsApp, YouTube, OTP) normal chalte rehte hain.
     */
    fun onAccessibilityEvent(service: AccessibilityService, event: AccessibilityEvent): Boolean {
        if (!running) return false
        if (index !in jobs.indices) return false
        if (event.packageName?.toString() != SETTINGS_PACKAGE) return false
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return false
        advance(service)
        return true
    }

    private fun currentProgress(): Progress {
        val done = results.count { it.status == JobStatus.DONE }
        val failed = results.count { it.status == JobStatus.FAILED }
        val skipped = results.count { it.status == JobStatus.SKIPPED }
        val current = if (index in jobs.indices) jobs[index].label else ""
        return Progress(jobs.size, done, failed, skipped, current, running, results.toList())
    }

    private fun publish(currentLabel: String) {
        _progress.value = currentProgress().copy(currentLabel = currentLabel)
        appContext?.let { AppManagerNotifier.showProgress(it, _progress.value!!) }
    }

    private fun setStep(newStep: Step) {
        step = newStep
        stepEnteredAt = System.currentTimeMillis()
    }

    private fun openNext() {
        if (!running) return
        index++
        if (index >= jobs.size) {
            finishAll()
            return
        }
        val job = jobs[index]
        setStep(Step.WAIT_DETAILS)
        publish(job.label)
        val ctx = appContext ?: return
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${job.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "cannot open app info for ${job.packageName}: ${e.message}")
            finishCurrent(JobStatus.FAILED, "App info page nahi khula")
            handler.postDelayed({ openNext() }, NEXT_APP_DELAY_MS)
        }
        // Agar Settings ka window event na aaye to watchdog 20s par fail
        // karke agle app par chala jayega. Advance() Settings ke har
        // window/content event par chalta hai.
    }

    private fun finishAll() {
        running = false
        handler.removeCallbacks(watchdog)
        step = Step.IDLE
        val final = currentProgress().copy(running = false, currentLabel = "")
        _progress.value = final
        val ctx = appContext
        if (ctx != null) {
            AppManagerNotifier.showFinished(ctx, final)
            // Misri wapas samne lao taaki user final log dekh sake.
            try {
                val intent = Intent(ctx, AppManagerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                ctx.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "cannot bring AppManagerActivity to front: ${e.message}")
            }
        }
        Log.i(TAG, "queue finished: ${final.done} done, ${final.failed} failed, ${final.skipped} skipped / ${final.total}")
    }

    private fun finishCurrent(status: JobStatus, note: String) {
        val job = jobs[index]
        results.add(JobResult(job.packageName, job.label, status, note))
        publish(job.label)
    }

    private fun advance(service: AccessibilityService) {
        val job = jobs.getOrNull(index) ?: return
        val root = service.rootInActiveWindow ?: return
        when (step) {
            Step.WAIT_DETAILS -> {
                if (findNode(root, "Force stop") == null && findNode(root, "Storage") == null) return
                when (job.action) {
                    CleanerAction.FORCE_STOP -> setStep(Step.CLICK_FORCE_STOP)
                    CleanerAction.CLEAR_CACHE, CleanerAction.CLEAR_DATA -> setStep(Step.CLICK_STORAGE_ROW)
                }
                advance(service)
            }
            Step.CLICK_FORCE_STOP -> {
                val node = findClickable(root, "Force stop") ?: return
                if (!node.isEnabled) {
                    finishCurrent(JobStatus.SKIPPED, "Force stop button band tha (pehle se band hai)")
                    handler.postDelayed({ openNext() }, NEXT_APP_DELAY_MS)
                    return
                }
                if (clickNode(node)) setStep(Step.WAIT_FORCE_CONFIRM) else return
            }
            Step.WAIT_FORCE_CONFIRM -> {
                // Dialog: "If you force stop an app..." — OK dabao.
                val ok = findClickable(root, "OK")
                if (ok != null && clickNode(ok)) {
                    finishCurrent(JobStatus.DONE, "Force stop ho gaya")
                    handler.postDelayed({ openNext() }, NEXT_APP_DELAY_MS)
                    return
                }
                // Dialog hi nahi aaya aur button gayab = pehle se stopped.
                if (findNode(root, "Force stop") == null) {
                    finishCurrent(JobStatus.DONE, "Force stop ho gaya")
                    handler.postDelayed({ openNext() }, NEXT_APP_DELAY_MS)
                }
            }
            Step.CLICK_STORAGE_ROW -> {
                val node = findClickable(root, "Storage & cache")
                    ?: findClickable(root, "Storage")
                    ?: return
                if (clickNode(node)) setStep(Step.WAIT_STORAGE_SCREEN)
            }
            Step.WAIT_STORAGE_SCREEN -> {
                when (job.action) {
                    CleanerAction.CLEAR_CACHE -> {
                        val node = findClickable(root, "Clear cache") ?: return
                        if (!node.isEnabled) {
                            finishCurrent(JobStatus.SKIPPED, "Cache 0 hai — clear karne ko kuch nahi")
                            handler.postDelayed({ openNext() }, NEXT_APP_DELAY_MS)
                            return
                        }
                        if (clickNode(node)) {
                            finishCurrent(JobStatus.DONE, "Cache clear ho gaya")
                            handler.postDelayed({ openNext() }, NEXT_APP_DELAY_MS)
                        }
                    }
                    CleanerAction.CLEAR_DATA -> {
                        val node = findClickable(root, "Clear storage")
                            ?: findClickable(root, "Clear data")
                            ?: return
                        if (!node.isEnabled) {
                            finishCurrent(JobStatus.SKIPPED, "Clear storage button band tha")
                            handler.postDelayed({ openNext() }, NEXT_APP_DELAY_MS)
                            return
                        }
                        if (clickNode(node)) setStep(Step.WAIT_DELETE_CONFIRM)
                    }
                    else -> Unit
                }
            }
            Step.WAIT_DELETE_CONFIRM -> {
                // Dialog: "Delete app data?" — Delete dabao, phir OK (agar aaye).
                val delete = findClickable(root, "Delete")
                if (delete != null && clickNode(delete)) {
                    finishCurrent(JobStatus.DONE, "Data clear ho gaya")
                    handler.postDelayed({ openNext() }, NEXT_APP_DELAY_MS)
                    return
                }
                val ok = findClickable(root, "OK")
                if (ok != null && clickNode(ok)) {
                    finishCurrent(JobStatus.DONE, "Data clear ho gaya")
                    handler.postDelayed({ openNext() }, NEXT_APP_DELAY_MS)
                }
            }
            Step.IDLE -> Unit
        }
    }

    // ---- Node helpers (existing JarvisAccessibilityService jaisa pattern:
    // findAccessibilityNodeInfosByText + clickable parent tak chadhna) ----

    private fun findNode(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val exact = root.findAccessibilityNodeInfosByText(text)
        if (exact.isNotEmpty()) return exact[0]
        return findNodeByWalk(root, text)
    }

    private fun findNodeByWalk(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodeText = node.text?.toString().orEmpty()
        val desc = node.contentDescription?.toString().orEmpty()
        if (nodeText.contains(text, ignoreCase = true) || desc.contains(text, ignoreCase = true)) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByWalk(child, text)
            if (found != null) return found
        }
        return null
    }

    /** Text wala node, ya uska sabse nazdeek clickable parent. */
    private fun findClickable(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val exact = root.findAccessibilityNodeInfosByText(text)
        for (candidate in exact) {
            var node: AccessibilityNodeInfo? = candidate
            while (node != null) {
                if (node.isClickable) return node
                node = node.parent
            }
        }
        val walked = findNodeByWalk(root, text) ?: return null
        var node: AccessibilityNodeInfo? = walked
        while (node != null) {
            if (node.isClickable) return node
            node = node.parent
        }
        return null
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        return try {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } catch (e: Exception) {
            Log.w(TAG, "click failed: ${e.message}")
            false
        }
    }
}
