package com.jarvis.assistant.appmanager

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jarvis.assistant.automation.accessibility.JarvisAccessibilityService
import com.jarvis.assistant.databinding.ActivityAppManagerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Misri Settings > Phone Cleaner / App Manager.
 *
 * Cute Pink theme (Misri default): pink #EC4899, halka pink background
 * #FFF0F6, dark text #4A1030 — colours layout me literal likhe hain taaki
 * kisi theme attr par depend na kare.
 *
 * Honest UI: har action par saaf likha hai ki kaam ek-ek karke hoga,
 * turant sab ek saath nahi, aur screen on rakhni hai.
 */
class AppManagerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ACTION = "extra_action" // CleanerAction.name
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_APP_LABEL = "extra_app_label"
        const val EXTRA_CLEAR_DATA_MODE = "extra_clear_data_mode"
    }

    private lateinit var binding: ActivityAppManagerBinding
    private var allApps: List<ManagedApp> = emptyList()
    private var visibleApps: List<ManagedApp> = emptyList()
    private val selected = LinkedHashSet<String>()
    private var filter: Filter = Filter.ALL
    private var query: String = ""
    private lateinit var adapter: AppManagerAdapter

    private enum class Filter { ALL, INSTALLED, SYSTEM }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AppManagerAdapter(
            onToggle = { app, checked ->
                if (app.isLocked) {
                    Toast.makeText(this, app.lockReason ?: "Ye app locked hai", Toast.LENGTH_SHORT).show()
                } else if (checked) {
                    selected.add(app.packageName)
                } else {
                    selected.remove(app.packageName)
                }
                updateSelectionSummary()
            }
        )
        binding.recyclerApps.layoutManager = LinearLayoutManager(this)
        binding.recyclerApps.adapter = adapter

        binding.etSearch.addTextChangedListener { text ->
            query = text?.toString().orEmpty()
            applyFilter()
        }
        binding.btnFilterAll.setOnClickListener { setFilter(Filter.ALL) }
        binding.btnFilterInstalled.setOnClickListener { setFilter(Filter.INSTALLED) }
        binding.btnFilterSystem.setOnClickListener { setFilter(Filter.SYSTEM) }

        binding.btnSelectAll.setOnClickListener {
            // Select All sirf safe apps ko select karta hai — locked kabhi nahi.
            visibleApps.filter { !it.isLocked }.forEach { selected.add(it.packageName) }
            adapter.updateSelection(selected)
            updateSelectionSummary()
            Toast.makeText(this, "Safe apps select ho gayi — locked system apps nahi", Toast.LENGTH_SHORT).show()
        }
        binding.btnSelectNone.setOnClickListener {
            selected.clear()
            adapter.updateSelection(selected)
            updateSelectionSummary()
        }

        binding.btnEnableAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, "Accessibility me Misri ko ON karo, phir wapas aao", Toast.LENGTH_LONG).show()
        }
        binding.btnEnableUsageAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            Toast.makeText(this, "Usage Access me Misri ko allow karo — tab cache/data size dikhega", Toast.LENGTH_LONG).show()
        }

        binding.btnForceStopAll.setOnClickListener { askAndStart(CleanerAction.FORCE_STOP) }
        binding.btnClearCacheAll.setOnClickListener { askAndStart(CleanerAction.CLEAR_CACHE) }
        binding.btnClearData.setOnClickListener { askClearData() }
        binding.btnStopQueue.setOnClickListener {
            AppManagerDriver.stop()
            Toast.makeText(this, "Queue rok di", Toast.LENGTH_SHORT).show()
        }

        loadApps()
        observeQueue()
        handleVoiceExtras(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleVoiceExtras(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionCard()
    }

    private fun loadApps() {
        lifecycleScope.launch {
            val entries = withContext(Dispatchers.IO) { AppListRepository.loadApps(this@AppManagerActivity) }
            allApps = entries.map { AppListRepository.toManaged(it) }
            applyFilter()
        }
    }

    private fun setFilter(newFilter: Filter) {
        filter = newFilter
        binding.btnFilterAll.alpha = if (filter == Filter.ALL) 1f else 0.55f
        binding.btnFilterInstalled.alpha = if (filter == Filter.INSTALLED) 1f else 0.55f
        binding.btnFilterSystem.alpha = if (filter == Filter.SYSTEM) 1f else 0.55f
        applyFilter()
    }

    private fun applyFilter() {
        val q = query.trim().lowercase()
        visibleApps = allApps.filter { app ->
            val matchesFilter = when (filter) {
                Filter.ALL -> true
                Filter.INSTALLED -> !app.isSystem
                Filter.SYSTEM -> app.isSystem
            }
            val matchesQuery = q.isBlank() ||
                app.label.lowercase().contains(q) ||
                app.packageName.lowercase().contains(q)
            matchesFilter && matchesQuery
        }
        adapter.submit(visibleApps, selected)
        binding.tvEmpty.visibility = if (visibleApps.isEmpty()) View.VISIBLE else View.GONE
        updateSelectionSummary()
    }

    private fun updateSelectionSummary() {
        val selectable = visibleApps.count { !it.isLocked }
        val totalCache = visibleApps.filter { selected.contains(it.packageName) && it.cacheBytes > 0 }
            .sumOf { it.cacheBytes }
        binding.tvSelectSummary.text = "${selected.size} selected (is list me $selectable safe) • " +
            "cache: ${AppListRepository.formatBytes(totalCache)}"
        val enabled = selected.isNotEmpty()
        binding.btnForceStopAll.isEnabled = enabled
        binding.btnClearCacheAll.isEnabled = enabled
        binding.btnClearData.isEnabled = enabled
        refreshPermissionCard()
    }

    private fun refreshPermissionCard() {
        val accessOn = JarvisAccessibilityService.isEnabled()
        val usageOn = AppListRepository.isUsageAccessGranted(this)
        binding.tvAccessStatus.text = if (accessOn) {
            "Accessibility: ON — Misri Settings khol kar buttons daba sakti hai"
        } else {
            "Accessibility: OFF — pehle ON karo, warna koi action chalega nahi"
        }
        binding.tvUsageStatus.text = if (usageOn) {
            "Usage Access: ON — cache/data size dikh raha hai"
        } else {
            "Usage Access: OFF — size nahi dikhega (optional, kaam phir bhi chalega)"
        }
        binding.btnEnableAccessibility.visibility = if (accessOn) View.GONE else View.VISIBLE
        binding.btnEnableUsageAccess.visibility = if (usageOn) View.GONE else View.VISIBLE
        // Action buttons tab tak disabled jab tak Accessibility ON na ho.
        val queueRunning = AppManagerDriver.isRunning()
        val actionsEnabled = accessOn && !queueRunning && selected.isNotEmpty()
        binding.btnForceStopAll.isEnabled = actionsEnabled
        binding.btnClearCacheAll.isEnabled = actionsEnabled
        binding.btnClearData.isEnabled = actionsEnabled
        binding.progressCard.visibility = if (queueRunning || AppManagerDriver.progress.value != null) View.VISIBLE else View.GONE
    }

    private fun selectedJobs(action: CleanerAction): List<AppManagerDriver.Job> {
        return allApps.filter { selected.contains(it.packageName) && !it.isLocked }
            .map { AppManagerDriver.Job(it.packageName, it.label, action) }
    }

    private fun askAndStart(action: CleanerAction) {
        if (!JarvisAccessibilityService.isEnabled()) {
            Toast.makeText(this, "Pehle Accessibility me Misri ON karo", Toast.LENGTH_LONG).show()
            return
        }
        if (AppManagerDriver.isRunning()) {
            Toast.makeText(this, "Ek queue pehle se chal rahi hai — pehle use roko", Toast.LENGTH_SHORT).show()
            return
        }
        val jobs = selectedJobs(action)
        if (jobs.isEmpty()) {
            Toast.makeText(this, "Pehle koi app select karo", Toast.LENGTH_SHORT).show()
            return
        }
        val what = if (action == CleanerAction.FORCE_STOP) "force stop" else "cache clear"
        MaterialAlertDialogBuilder(this)
            .setTitle("${jobs.size} apps ka $what karun?")
            .setMessage(
                "Misri ek-ek karke Settings khol kar $what karegi — turant sab ek saath nahi hoga. " +
                    "Har app par 2-4 second lagenge. Screen on rakhna aur beech me Settings ko mat chhedna.\n\n" +
                    "Notifications us waqt tak ruk sakti hain jab tak app dobara na khule."
            )
            .setPositiveButton("Shuru karo") { _, _ -> startQueue(jobs) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** Clear Data: do alag confirmations, Select All par ek extra. */
    private fun askClearData() {
        if (!JarvisAccessibilityService.isEnabled()) {
            Toast.makeText(this, "Pehle Accessibility me Misri ON karo", Toast.LENGTH_LONG).show()
            return
        }
        if (AppManagerDriver.isRunning()) {
            Toast.makeText(this, "Ek queue pehle se chal rahi hai — pehle use roko", Toast.LENGTH_SHORT).show()
            return
        }
        val jobs = selectedJobs(CleanerAction.CLEAR_DATA)
        if (jobs.isEmpty()) {
            Toast.makeText(this, "Pehle koi app select karo", Toast.LENGTH_SHORT).show()
            return
        }
        val names = jobs.take(5).joinToString(", ") { it.label } +
            if (jobs.size > 5) " aur ${jobs.size - 5} apps" else ""
        MaterialAlertDialogBuilder(this)
            .setTitle("DANGER — Data clear hoga!")
            .setMessage(
                "Ye ${jobs.size} apps ka saara data delete kar dega: $names\n\n" +
                    "• Har app se logout ho jayega, dobara login karna padega\n" +
                    "• App ki settings reset ho jayengi\n" +
                    "• WhatsApp ke chats backup na ho to hamesha ke liye chale jayenge\n" +
                    "• Banking / UPI apps me dobara setup karna padega\n\n" +
                    "Ye wapas nahi aayega. Pakka karna hai?"
            )
            .setPositiveButton("Haan, samajh gaya") { _, _ -> askClearDataSecond(jobs) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun askClearDataSecond(jobs: List<AppManagerDriver.Job>) {
        val allSelected = jobs.size == allApps.count { !it.isLocked }
        val message = if (allSelected) {
            "Tumne Select All kiya hai — SABHI safe apps ka data ek-ek karke clear hoga. " +
                "Ye bahut bada step hai. Aakhri baar pooch rahi hoon: shuru karun?"
        } else {
            "Aakhri confirmation: ${jobs.size} apps ka data clear karun?"
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Aakhri baar confirm karo")
            .setMessage(message)
            .setPositiveButton("Haan, shuru karo") { _, _ -> startQueue(jobs) }
            .setNegativeButton("Ruko, cancel", null)
            .show()
    }

    private fun startQueue(jobs: List<AppManagerDriver.Job>) {
        val started = AppManagerDriver.start(this, jobs)
        if (!started) {
            Toast.makeText(this, "Queue shuru nahi hui — Accessibility ON hai ya nahi, check karo", Toast.LENGTH_LONG).show()
            return
        }
        binding.progressCard.visibility = View.VISIBLE
        Toast.makeText(this, "Shuru ho gaya — ek-ek karke hoga, screen on rakhna", Toast.LENGTH_LONG).show()
    }

    private fun observeQueue() {
        lifecycleScope.launch {
            AppManagerDriver.progress.collect { progress ->
                if (progress == null) {
                    binding.progressCard.visibility = View.GONE
                    return@collect
                }
                binding.progressCard.visibility = View.VISIBLE
                binding.tvProgressCurrent.text = if (progress.running) {
                    "Kar rahi hoon: ${progress.currentLabel}"
                } else {
                    "Khatam — ${progress.done} done, ${progress.failed} fail, ${progress.skipped} skip"
                }
                binding.progressBar.max = progress.total.coerceAtLeast(1)
                binding.progressBar.progress = progress.done + progress.failed + progress.skipped
                binding.tvProgressCounts.text =
                    "Done: ${progress.done} • Fail: ${progress.failed} • Skip: ${progress.skipped} • Total: ${progress.total}"
                binding.btnStopQueue.visibility = if (progress.running) View.VISIBLE else View.GONE
                if (progress.log.isNotEmpty()) {
                    binding.tvLog.text = progress.log.joinToString("\n") { result ->
                        val mark = when (result.status) {
                            JobStatus.DONE -> "✓"
                            JobStatus.FAILED -> "✗"
                            JobStatus.SKIPPED -> "–"
                        }
                        "$mark ${result.label}: ${result.note}"
                    }
                }
                if (!progress.running) refreshPermissionCard()
            }
        }
    }

    /** Voice se aaya: single app pre-select + confirm; Clear Data sirf screen. */
    private fun handleVoiceExtras(intent: Intent) {
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: return
        val label = intent.getStringExtra(EXTRA_APP_LABEL) ?: pkg
        val actionName = intent.getStringExtra(EXTRA_ACTION)
        val clearDataMode = intent.getBooleanExtra(EXTRA_CLEAR_DATA_MODE, false)
        lifecycleScope.launch {
            // Apps load hone ka wait karo (max ~5s).
            var waits = 0
            while (allApps.isEmpty() && waits < 50) {
                kotlinx.coroutines.delay(100)
                waits++
            }
            val app = allApps.firstOrNull { it.packageName == pkg } ?: return@launch
            if (app.isLocked) {
                Toast.makeText(this@AppManagerActivity, "$label locked hai — ye system app hai", Toast.LENGTH_LONG).show()
                return@launch
            }
            selected.clear()
            selected.add(pkg)
            applyFilter()
            if (clearDataMode) {
                Toast.makeText(
                    this@AppManagerActivity,
                    "$label select ho gaya — data clear karne ke liye Danger section se khud confirm karo",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            val action = try {
                CleanerAction.valueOf(actionName ?: "")
            } catch (_: Exception) {
                return@launch
            }
            val what = if (action == CleanerAction.FORCE_STOP) "force stop" else "cache clear"
            MaterialAlertDialogBuilder(this@AppManagerActivity)
                .setTitle("$label ko $what karun?")
                .setMessage("Misri Settings khol kar $label ka $what karegi — ek-ek karke, screen on rakhna.")
                .setPositiveButton("Haan, karo") { _, _ ->
                    startQueue(listOf(AppManagerDriver.Job(pkg, label, action)))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
