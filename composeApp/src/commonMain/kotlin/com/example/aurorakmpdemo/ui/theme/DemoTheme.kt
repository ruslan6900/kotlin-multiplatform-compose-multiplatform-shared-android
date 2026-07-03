package com.example.aurorakmpdemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun DemoTheme(content: @Composable () -> Unit) {
    val fontFamily = demoFontFamily()

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = DemoColors.textAccent,
            secondary = DemoColors.textSupport,
            tertiary = DemoColors.statusCyan,
            background = DemoColors.superSmokeBackground,
            surface = DemoColors.panelNavy,
            onPrimary = DemoColors.textPrimary,
            onSecondary = DemoColors.textPrimary,
            onTertiary = DemoColors.textPrimary,
            onBackground = DemoColors.textPrimary,
            onSurface = DemoColors.textPrimary,
        ),
        typography = Typography(
            displayLarge = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 38.sp,
            ),
            titleLarge = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 26.sp,
            ),
            titleMedium = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 22.sp,
            ),
            bodyLarge = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 20.sp,
            ),
            bodyMedium = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            ),
            labelLarge = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 18.sp,
            ),
        ),
        content = content,
    )
}
