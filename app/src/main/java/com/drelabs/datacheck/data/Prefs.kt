package com.drelabs.datacheck.data

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("ApplySharedPref")
class Prefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("datacheck_prefs", Context.MODE_PRIVATE)

    var lastTickEndMs: Long
        get() = sp.getLong(KEY_LAST_TICK, 0L)
        set(value) {
            sp.edit().putLong(KEY_LAST_TICK, value).commit()
        }

    var rebootPending: Boolean
        get() = sp.getBoolean(KEY_REBOOT, false)
        set(value) {
            sp.edit().putBoolean(KEY_REBOOT, value).commit()
        }

    companion object {
        private const val KEY_LAST_TICK = "last_tick_end_ms"
        private const val KEY_REBOOT = "reboot_pending"
    }
}
