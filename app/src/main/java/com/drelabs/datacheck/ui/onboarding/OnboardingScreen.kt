package com.drelabs.datacheck.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var usageGranted by remember { mutableStateOf(UsageAccess.isGranted(context)) }
    var notificationsGranted by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsGranted = granted
    }

    val usageAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        usageGranted = UsageAccess.isGranted(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("DataCheck", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Track your mobile data per app with a ping every 15 minutes. " +
                "Two one-time permissions are needed:",
            style = MaterialTheme.typography.bodyMedium,
        )

        PermissionRow(
            title = "1. Usage access",
            body = "Lets DataCheck read per-app mobile data and foreground/background stats.",
            granted = usageGranted,
            buttonText = if (usageGranted) "Granted" else "Open Settings",
            enabled = !usageGranted,
        ) {
            usageAccessLauncher.launch(UsageAccess.settingsIntent())
        }

        PermissionRow(
            title = "2. Notifications",
            body = "Required for the 15-minute data ping notification.",
            granted = notificationsGranted,
            buttonText = if (notificationsGranted) "Granted" else "Allow",
            enabled = !notificationsGranted,
        ) {
            if (Build.VERSION.SDK_INT >= 33) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                notificationsGranted = true
            }
        }

        Button(
            onClick = onDone,
            enabled = usageGranted,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (usageGranted) "Continue" else "Waiting for usage access…")
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    body: String,
    granted: Boolean,
    buttonText: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(body, style = MaterialTheme.typography.bodySmall)
        Button(onClick = onClick, enabled = enabled) {
            Text(buttonText)
        }
    }
}
