package com.drelabs.datacheck.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.drelabs.datacheck.R

val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.W400),
    Font(R.font.geist_medium, FontWeight.W500),
    Font(R.font.geist_semibold, FontWeight.W600),
)

private val GeistTypography = Typography(
    displayLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.W400, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.W400, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
    displaySmall = TextStyle(fontFamily = Geist, fontWeight = FontWeight.W400, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.W400, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.W400, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Geist, fontWeight = FontWeight.W400, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.W500, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.W500, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = Geist, fontWeight = FontWeight.W500, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.W400, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.W400, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = Geist, fontWeight = FontWeight.W400, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.W500, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.W500, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = Geist, fontWeight = FontWeight.W500, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF475569),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF1E293B),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF0C4A6E),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

@Composable
fun DataCheckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = GeistTypography,
        content = content,
    )
}
