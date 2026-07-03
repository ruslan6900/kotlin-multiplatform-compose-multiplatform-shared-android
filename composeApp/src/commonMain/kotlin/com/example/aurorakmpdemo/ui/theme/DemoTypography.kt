package com.example.aurorakmpdemo.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.aurorakmpdemo.resources.*
import org.jetbrains.compose.resources.Font

data class DemoTypography(
    val hero: TextStyle,
    val subtitle: TextStyle,
    val section: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val button: TextStyle,
)

@Composable
fun demoFontFamily(): FontFamily = FontFamily(
    Font(Res.font.golos_text_regular, FontWeight.Normal),
    Font(Res.font.golos_text_medium, FontWeight.Medium),
    Font(Res.font.golos_text_bold, FontWeight.Bold),
)

@Composable
fun demoTypography(): DemoTypography {
    val demoFontFamily = demoFontFamily()

    return DemoTypography(
        hero = TextStyle(
            fontFamily = demoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 38.sp,
        ),
        subtitle = TextStyle(
            fontFamily = demoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 26.sp,
        ),
        section = TextStyle(
            fontFamily = demoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 22.sp,
        ),
        body = TextStyle(
            fontFamily = demoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        ),
        caption = TextStyle(
            fontFamily = demoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 18.sp,
        ),
        button = TextStyle(
            fontFamily = demoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 18.sp,
        ),
    )
}
