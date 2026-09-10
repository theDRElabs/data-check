package com.drelabs.datacheck

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.drelabs.datacheck.ui.DataCheckTheme
import com.drelabs.datacheck.ui.dashboard.DashboardScreen
import com.drelabs.datacheck.ui.onboarding.OnboardingScreen
import com.drelabs.datacheck.ui.settings.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            DataCheckTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var onboarded by remember { mutableStateOf(false) }
                    var showSettings by remember { mutableStateOf(false) }
                    BackHandler(enabled = onboarded && showSettings) {
                        showSettings = false
                    }
                    if (onboarded) {
                        if (showSettings) {
                            SettingsScreen(onBack = { showSettings = false })
                        } else {
                            DashboardScreen(onOpenSettings = { showSettings = true })
                        }
                    } else {
                        OnboardingScreen(onDone = { onboarded = true })
                    }
                }
            }
        }
    }
}
