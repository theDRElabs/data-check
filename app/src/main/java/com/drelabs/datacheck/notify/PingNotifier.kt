package com.drelabs.datacheck.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.drelabs.datacheck.MainActivity
import com.drelabs.datacheck.R
import com.drelabs.datacheck.util.Format

object PingNotifier {
    private const val CHANNEL_ID = "ping"
    private const val NOTIFICATION_ID = 1

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Data pings",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Every-15-minutes mobile data summary"
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun show(
        context: Context,
        totalTodayBytes: Long,
        windowDeltaBytes: Long,
        topApps: List<Pair<String, Long>>,
    ) {
        ensureChannel(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val style = NotificationCompat.InboxStyle()
        topApps.forEach { (pkg, bytes) ->
            style.addLine("${Format.bytes(bytes)} — $pkg")
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_ping)
            .setContentTitle("Mobile today: ${Format.bytes(totalTodayBytes)}")
            .setContentText("+${Format.bytes(windowDeltaBytes)} in last window")
            .setStyle(style)
            .setOnlyAlertOnce(true)
            .setOngoing(false)
            .setContentIntent(contentIntent)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }
}
