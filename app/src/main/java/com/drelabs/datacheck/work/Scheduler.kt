package com.drelabs.datacheck.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.drelabs.datacheck.data.Prefs
import java.util.concurrent.TimeUnit

object Scheduler {
    const val MIN_MINUTES = 15L

    fun ensurePeriodicTick(context: Context) {
        val minutes = Prefs(context).pingIntervalMinutes.coerceAtLeast(MIN_MINUTES.toInt())
        val request = PeriodicWorkRequestBuilder<TickWorker>(minutes.toLong(), TimeUnit.MINUTES).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            TickWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun updateInterval(context: Context, minutes: Int) {
        val safe = minutes.coerceAtLeast(MIN_MINUTES.toInt())
        Prefs(context).pingIntervalMinutes = safe
        val request = PeriodicWorkRequestBuilder<TickWorker>(safe.toLong(), TimeUnit.MINUTES).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            TickWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
