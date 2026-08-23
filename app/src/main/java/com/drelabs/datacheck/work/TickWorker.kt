package com.drelabs.datacheck.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.drelabs.datacheck.data.SamplingEngine
import com.drelabs.datacheck.data.db.UsageLogDb
import com.drelabs.datacheck.notify.PingNotifier
import com.drelabs.datacheck.util.AppLabels
import java.time.LocalDate
import java.time.ZoneId

class TickWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val engine = SamplingEngine(applicationContext)
        val result = engine.runTick()
        postPing(applicationContext, result)
        Result.success()
    } catch (t: Throwable) {
        if (runAttemptCount < 3) Result.retry() else Result.success()
    }

    private suspend fun postPing(
        context: Context,
        result: SamplingEngine.TickResult?,
    ) {
        val dao = UsageLogDb.get(context).usageLogDao()
        val midnight = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val todayTotal = dao.totalsSince(midnight)?.total ?: 0L
        val windowDelta = result?.rows?.sumOf { it.rx + it.tx } ?: 0L
        val topApps = dao.topAppsSince(midnight, 3).map {
            AppLabels.label(context, it.pkg) to it.total
        }
        PingNotifier.show(context, todayTotal, windowDelta, topApps)
    }

    companion object {
        const val UNIQUE_NAME = "datacheck-tick"
    }
}
