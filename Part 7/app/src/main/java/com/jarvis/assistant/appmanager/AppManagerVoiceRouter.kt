package com.jarvis.assistant.appmanager

import android.content.Context
import android.content.Intent
import android.util.Log
import com.jarvis.assistant.JarvisApp
import java.util.Locale

/**
 * Misri voice commands -> Phone Cleaner.
 *
 * CommandParser (section 2b, patch se judi) inhe JarvisCommand me badalta
 * hai, CommandExecutor yahan route karta hai. Clear Data voice se KABHI
 * execute nahi hota — sirf selection screen khulti hai.
 *
 * Phrases (Hinglish + English, lowercase match):
 *  - "sab apps ka cache clear karo" / "sabka cache clear karo" / "clear cache of all apps"
 *  - "sab apps ko force stop karo" / "saare apps band karo" / "force stop all apps"
 *  - "[app] ko force stop karo" / "force stop [app]"
 *  - "[app] ka cache clear karo" / "clear cache of [app]"
 *  - "[app] ka data clear karo" / "sab apps ka data clear karo" -> sirf screen kholo
 */
object AppManagerVoiceRouter {

    private const val TAG = "MisriAppManager"

    sealed class VoiceRoute {
        data class OpenManager(
            val action: CleanerAction?,
            val packageName: String?,
            val appLabel: String?
        ) : VoiceRoute()

        data class AppNotFound(val spokenName: String) : VoiceRoute()
        object NotMatched : VoiceRoute()
    }

    /** CommandExecutor se: JarvisCommand ko screen-action me badlo. */
    fun routeFromCommand(
        action: CleanerAction?,
        packageName: String?,
        appLabel: String?,
        clearDataMode: Boolean
    ): String {
        val ctx: Context = JarvisApp.instance.applicationContext
        // Single-app voice command me sirf naam aata hai — package yahan resolve karo.
        var resolvedPkg = packageName
        var resolvedLabel = appLabel
        if (resolvedPkg == null && appLabel != null) {
            val found = AppListRepository.resolveByName(ctx, appLabel)
            if (found == null) {
                return "Kaunsi app? Mujhe '$appLabel' naam ki app nahi mili. Sahi naam bolo ya Phone Cleaner me khud select karo."
            }
            if (AppManagerExclusions.isExcluded(found.packageName)) {
                return "'$appLabel' system app hai, use force stop ya clear nahi kar sakti — phone band ho sakta hai."
            }
            resolvedPkg = found.packageName
            resolvedLabel = found.label
        }
        val intent = Intent(ctx, AppManagerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (clearDataMode) putExtra(AppManagerActivity.EXTRA_CLEAR_DATA_MODE, true)
            if (action != null) putExtra(AppManagerActivity.EXTRA_ACTION, action.name)
            if (resolvedPkg != null) putExtra(AppManagerActivity.EXTRA_PACKAGE, resolvedPkg)
            if (resolvedLabel != null) putExtra(AppManagerActivity.EXTRA_APP_LABEL, resolvedLabel)
        }
        return try {
            ctx.startActivity(intent)
            when {
                clearDataMode -> "Phone Cleaner khol rahi hoon — data clear karne se pehle tum khud select karke confirm karna, main khud se data clear nahi karungi."
                action == CleanerAction.CLEAR_CACHE && resolvedPkg == null ->
                    "Phone Cleaner khol rahi hoon, sab apps ka cache ek-ek karke clear karne ke liye confirm karo."
                action == CleanerAction.FORCE_STOP && resolvedPkg == null ->
                    "Phone Cleaner khol rahi hoon, sab apps ko force stop karne ke liye confirm karo."
                resolvedPkg != null ->
                    "Phone Cleaner khol rahi hoon, ${resolvedLabel ?: resolvedPkg} ke liye confirm karo."
                else -> "Phone Cleaner khol rahi hoon."
            }
        } catch (e: Exception) {
            Log.e(TAG, "cannot open AppManagerActivity: ${e.message}")
            "Phone Cleaner khol nahi payi, Settings se kholo."
        }
    }

    /** Seedha text se route karo (test / Gemini fallback ke liye). */
    fun routeFromText(context: Context, rawText: String): VoiceRoute {
        val clean = rawText.trim().lowercase(Locale.ROOT)
            .replace(Regex("^(hey misri|hi misri|hello misri|misri|please)\\s*,?\\s*"), "")
            .trim()
        if (clean.isBlank()) return VoiceRoute.NotMatched

        val allCache = clean.matches(Regex(".*(sab|saare|sare|all)\\s+(apps?|app|ka|ki)?\\s*(ka\\s+)?cache\\s*(clear|saaf)\\s*(karo|kar\\s*do|karde)?.*")) ||
            clean.contains("sabka cache clear") || clean.contains("clear cache of all apps") ||
            clean == "sab apps ka cache clear karo" || clean == "sabka cache saaf karo"
        if (allCache) return VoiceRoute.OpenManager(CleanerAction.CLEAR_CACHE, null, null)

        val allForce = clean.contains("sab apps ko force stop") || clean.contains("saare apps") && clean.contains("force stop") ||
            clean.contains("force stop all apps") || clean.contains("sab apps band karo") ||
            clean.contains("saare apps band karo")
        if (allForce) return VoiceRoute.OpenManager(CleanerAction.FORCE_STOP, null, null)

        val allData = clean.contains("sab apps ka data clear") || clean.contains("saare apps ka data")
        if (allData) return VoiceRoute.OpenManager(null, null, null) // clear-data mode flag Activity me set hota hai

        // Single app: "[naam] ko force stop karo" / "[naam] ka cache clear karo" / "[naam] ka data clear karo"
        val singleForce = Regex("^(.*?)\\s+(ko\\s+)?force stop\\s*(karo|kar\\s*do|karde)?$").find(clean)
        if (singleForce != null) {
            val name = singleForce.groupValues[1].trim()
            return resolveSingle(context, name, CleanerAction.FORCE_STOP)
        }
        val singleCache = Regex("^(.*?)\\s+(ka\\s+)?cache\\s*(clear|saaf)\\s*(karo|kar\\s*do|karde)?$").find(clean)
        if (singleCache != null) {
            val name = singleCache.groupValues[1].trim()
            return resolveSingle(context, name, CleanerAction.CLEAR_CACHE)
        }
        val singleData = Regex("^(.*?)\\s+(ka\\s+)?data\\s*(clear|delete|saaf)\\s*(karo|kar\\s*do|karde)?$").find(clean)
        if (singleData != null) {
            val name = singleData.groupValues[1].trim()
            val found = AppListRepository.resolveByName(context, name)
            if (found == null) return VoiceRoute.AppNotFound(name)
            // Data: sirf screen kholo, kabhi auto-run nahi.
            return VoiceRoute.OpenManager(null, found.packageName, found.label)
        }
        val forcePrefix = Regex("^force stop\\s+(.+)$").find(clean)
        if (forcePrefix != null) {
            return resolveSingle(context, forcePrefix.groupValues[1].trim(), CleanerAction.FORCE_STOP)
        }
        val cachePrefix = Regex("^clear cache( of)?\\s+(.+)$").find(clean)
        if (cachePrefix != null) {
            return resolveSingle(context, cachePrefix.groupValues[2].trim(), CleanerAction.CLEAR_CACHE)
        }
        return VoiceRoute.NotMatched
    }

    private fun resolveSingle(context: Context, name: String, action: CleanerAction): VoiceRoute {
        if (name.isBlank() || name == "sab" || name == "saare") return VoiceRoute.NotMatched
        val found = AppListRepository.resolveByName(context, name)
        if (found == null) return VoiceRoute.AppNotFound(name)
        if (AppManagerExclusions.isExcluded(found.packageName)) {
            return VoiceRoute.AppNotFound(name) // locked app ka naam le kar bhi nahi
        }
        return VoiceRoute.OpenManager(action, found.packageName, found.label)
    }
}
