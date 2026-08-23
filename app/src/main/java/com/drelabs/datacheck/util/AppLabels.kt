package com.drelabs.datacheck.util

import android.content.Context

object AppLabels {
    private val cache = HashMap<String, String>()

    fun label(context: Context, pkg: String): String {
        when {
            pkg == "tethering" -> return "Tethering / Hotspot"
            pkg == "removed-apps" -> return "Removed apps"
            pkg == "android-system" -> return "Android System"
            pkg.startsWith("system-") -> return "System (uid " + pkg.removePrefix("system-") + ")"
            pkg.startsWith("uid-") -> return "App (" + pkg.replaceFirst("uid-", "uid ") + ")"
        }
        cache[pkg]?.let { return it }
        return try {
            val pm = context.packageManager
            val label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            cache[pkg] = label
            label
        } catch (_: Exception) {
            pkg
        }
    }
}
