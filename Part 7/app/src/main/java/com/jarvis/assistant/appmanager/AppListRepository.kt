package com.jarvis.assistant.appmanager

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import android.util.Log
import java.util.Locale

/**
 * Installed + system apps ki list PackageManager se, aur unke cache/data
 * size StorageStatsManager se.
 *
 * Size ke liye PACKAGE_USAGE_STATS chahiye hota hai — ye permission Play
 * Store se nahi milti, user ko Settings > Usage Access me Misri ko allow
 * karna padta hai. Agar allow nahi hai to size -1 (unknown) aata hai aur
 * list bina size ke dikhti hai — kaam phir bhi chalta hai.
 * QUERY_ALL_PACKAGES pehle se Misri ke Manifest me hai (Run #6).
 */
object AppListRepository {

    private const val TAG = "MisriAppManager"

    data class AppEntry(
        val packageName: String,
        val label: String,
        val isSystem: Boolean,
        val icon: Drawable?,
        val cacheBytes: Long,
        val dataBytes: Long
    )

    fun isUsageAccessGranted(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    /** Sizes ke liye best-effort: permission na ho to (-1, -1). */
    private fun querySizes(context: Context, packageName: String): Pair<Long, Long> {
        if (!isUsageAccessGranted(context)) return Pair(-1L, -1L)
        return try {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val uuid = storageManager.getUuidForPath(context.filesDir)
            val statsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val stats = statsManager.queryStatsForPackage(uuid, packageName, android.os.UserHandle.getUserHandleForUid(Process.myUid()))
            Pair(stats.cacheBytes, stats.dataBytes)
        } catch (e: Exception) {
            Log.d(TAG, "size query failed for $packageName: ${e.message}")
            Pair(-1L, -1L)
        }
    }

    @Suppress("DEPRECATION")
    fun loadApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val installed = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getInstalledApplications failed: ${e.message}")
            return emptyList()
        }
        val out = ArrayList<AppEntry>(installed.size)
        for (ai in installed) {
            val label = try {
                pm.getApplicationLabel(ai).toString()
            } catch (_: Exception) {
                ai.packageName
            }
            val icon = try {
                pm.getApplicationIcon(ai)
            } catch (_: Exception) {
                null
            }
            val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val (cache, data) = querySizes(context, ai.packageName)
            out.add(AppEntry(ai.packageName, label, isSystem, icon, cache, data))
        }
        out.sortWith(compareBy({ it.label.lowercase(Locale.ROOT) }))
        return out
    }

    fun toManaged(entry: AppEntry): ManagedApp {
        val locked = AppManagerExclusions.isExcluded(entry.packageName)
        return ManagedApp(
            packageName = entry.packageName,
            label = entry.label,
            isSystem = entry.isSystem,
            cacheBytes = entry.cacheBytes,
            dataBytes = entry.dataBytes,
            isLocked = locked,
            lockReason = AppManagerExclusions.lockReason(entry.packageName),
            icon = entry.icon
        )
    }

    /**
     * Voice se aaye app naam ko package se milao.
     * Pehle exact label, phir label contains, phir package contains, phir aliases.
     */
    fun resolveByName(context: Context, spokenName: String): AppEntry? {
        val query = spokenName.trim().lowercase(Locale.ROOT)
        if (query.isBlank()) return null
        val alias = AppNameAliases.resolve(query) ?: query
        val apps = loadApps(context)
        apps.firstOrNull { it.label.lowercase(Locale.ROOT) == alias }?.let { return it }
        apps.firstOrNull { it.packageName.lowercase(Locale.ROOT) == alias }?.let { return it }
        apps.firstOrNull { it.label.lowercase(Locale.ROOT).contains(alias) }?.let { return it }
        apps.firstOrNull { it.packageName.lowercase(Locale.ROOT).contains(alias) }?.let { return it }
        return null
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "—"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        return "%.2f GB".format(mb / 1024.0)
    }
}

/** Voice me bole jaane wale aam naam -> package/label keyword. */
object AppNameAliases {
    private val MAP = mapOf(
        "instagram" to "instagram",
        "insta" to "instagram",
        "whatsapp" to "whatsapp",
        "facebook" to "facebook",
        "fb" to "facebook",
        "youtube" to "youtube",
        "chrome" to "chrome",
        "telegram" to "telegram",
        "gmail" to "gmail",
        "maps" to "maps",
        "spotify" to "spotify",
        "snapchat" to "snapchat",
        "twitter" to "twitter",
        "x" to "twitter",
        "play store" to "google play store",
        "phonepe" to "phonepe",
        "paytm" to "paytm",
        "gpay" to "gpay",
        "google pay" to "gpay",
        "discord" to "discord",
        "gallery" to "gallery",
        "camera" to "camera",
        "clock" to "clock",
        "calendar" to "calendar",
        "drive" to "drive",
        "photos" to "photos"
    )

    fun resolve(spoken: String): String? = MAP[spoken.trim().lowercase(Locale.ROOT)]
}
