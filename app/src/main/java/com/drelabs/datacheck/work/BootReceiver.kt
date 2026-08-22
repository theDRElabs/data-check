package com.drelabs.datacheck.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.drelabs.datacheck.data.Prefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Prefs(context).rebootPending = true
            Scheduler.ensurePeriodicTick(context)
        }
    }
}
