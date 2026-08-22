package com.drelabs.datacheck.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.drelabs.datacheck.data.SamplingEngine

class TickWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        SamplingEngine(applicationContext).runTick()
        Result.success()
    } catch (t: Throwable) {
        if (runAttemptCount < 3) Result.retry() else Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "datacheck-tick"
    }
}
