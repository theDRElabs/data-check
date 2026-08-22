package com.drelabs.datacheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.drelabs.datacheck.ui.DataCheckTheme
import com.drelabs.datacheck.ui.onboarding.OnboardingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DataCheckTheme {
                var onboarded by remember { mutableStateOf(false) }
                if (onboarded) {
                    PlaceholderDashboard()
                } else {
                    OnboardingScreen(onDone = { onboarded = true })
                }
            }
        }
    }
}
