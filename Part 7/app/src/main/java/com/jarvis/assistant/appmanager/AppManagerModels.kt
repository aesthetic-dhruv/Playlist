package com.jarvis.assistant.appmanager

/**
 * Misri Phone Cleaner / App Manager — models + safety rules.
 *
 * HONEST LIMIT (Android, stock, no root): koi bhi normal app kisi dusri
 * app ko ek tap me force-stop / cache-clear / data-clear NAHI kar sakti.
 * FORCE_STOP_PACKAGES sirf system/signature apps ke liye hai.
 * Isliye Misri Baxa / Hibernator wale tareeke se kaam karti hai: ek tap se
 * queue shuru hoti hai, phir AccessibilityService Settings app khol kar
 * ek-ek app ka "Force stop" / "Clear cache" / "Clear storage" button
 * dabata hai. Andar ka kaam ek-ek karke hota hai, turant sab ek saath nahi.
 *
 * Reference pattern: Baxa: Close apps (com.tafayor.baxa, APPDEV QUEBEC,
 * Hibernator ke makers) — one-tap start, Accessibility Settings
 * automation, Exception List. Code ya branding copy nahi ki gayi hai,
 * sirf user experience match ki gayi hai. Baxa sirf force-stop karta hai;
 * Misri me cache aur data ke liye alag, dheemi queues hain, kyunki unme
 * extra Settings navigation aur confirm dialogs hote hain.
 */
enum class CleanerAction {
    FORCE_STOP,
    CLEAR_CACHE,
    CLEAR_DATA
}

data class ManagedApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val cacheBytes: Long, // -1 = unknown (Usage Access off)
    val dataBytes: Long, // -1 = unknown
    val isLocked: Boolean,
    val lockReason: String?,
    val icon: android.graphics.drawable.Drawable? = null // sirf memory me, list ke liye
) {
    val totalBytes: Long get() = if (cacheBytes < 0 || dataBytes < 0) -1 else cacheBytes + dataBytes
}

/** Ek app par kiye gaye kaam ka natija — Activity ke end-log me dikhta hai. */
enum class JobStatus { DONE, FAILED, SKIPPED }

data class JobResult(
    val packageName: String,
    val label: String,
    val status: JobStatus,
    val note: String
)

/**
 * Hard-excluded packages — inhe user select hi nahi kar sakta.
 * Inhe force-stop / data-clear karne se phone ka system, calls, ya
 * Misri khud band ho sakti hai.
 */
object AppManagerExclusions {
    const val MISRI_PACKAGE = "com.jarvis.assistant"

    private val HARD_EXCLUDED = setOf(
        "android",
        "com.android.systemui", // status bar, notifications, lock screen
        "com.android.phone", // calls
        "com.android.settings", // Settings khud — isi se automation chalti hai
        "com.google.android.gms", // Google Play Services — sab kuch is par chalta hai
        MISRI_PACKAGE // Misri khud
    )

    fun isExcluded(packageName: String): Boolean {
        if (packageName in HARD_EXCLUDED) return true
        // com.android.* — Android ke core system packages (providers, shell, etc.)
        if (packageName.startsWith("com.android.")) return true
        // Launcher — home screen chala jayega
        if (packageName == "com.google.android.apps.nexuslauncher") return true
        if (packageName.contains("launcher", ignoreCase = true)) return true
        return false
    }

    fun lockReason(packageName: String): String? {
        if (!isExcluded(packageName)) return null
        return when (packageName) {
            "android" -> "Android system — band kiya to phone chalega nahi"
            "com.android.systemui" -> "System UI — status bar aur notifications band ho jayenge"
            "com.android.phone" -> "Phone — calls band ho jayengi"
            "com.android.settings" -> "Settings — isi se Cleaner kaam karta hai"
            "com.google.android.gms" -> "Play Services — bahut si apps is par chalti hain"
            MISRI_PACKAGE -> "Misri khud — apne aap ko band nahi kar sakti"
            else -> if (packageName.contains("launcher", ignoreCase = true)) {
                "Launcher — home screen band ho jayega"
            } else {
                "System package — band karna safe nahi hai"
            }
        }
    }
}
