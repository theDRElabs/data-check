package com.drelabs.datacheck.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object Scheduler {
    fun ensurePeriodicTick(context: Context) {
        val request = PeriodicWorkRequestBuilder<TickWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            TickWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
