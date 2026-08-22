package com.drelabs.datacheck

import android.app.Application
import androidx.work.Configuration
import com.drelabs.datacheck.work.Scheduler

class DataCheckApp : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        Scheduler.ensurePeriodicTick(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
