package com.drelabs.datacheck.data

import android.content.Context
import androidx.core.content.FileProvider
import com.drelabs.datacheck.data.db.ExportRow
import com.drelabs.datacheck.data.db.UsageLogDb
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {
    private const val HEADER = "tick_start,tick_end,uid,package,rx_bytes,tx_bytes,fg_rx_bytes,fg_tx_bytes"

    suspend fun export(context: Context): File? {
        val rows = UsageLogDb.get(context).usageLogDao().exportRows()
        if (rows.isEmpty()) return null
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "datacheck_$stamp.csv")
        file.bufferedWriter().use { out ->
            out.appendLine(HEADER)
            rows.forEach { out.appendLine(it.toCsvLine()) }
        }
        return file
    }

    fun shareUri(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun ExportRow.toCsvLine(): String =
        listOf(tickStart, tickEnd, uid, pkg, rx, tx, fgRx, fgTx)
            .joinToString(",") { it.toString() }
}
