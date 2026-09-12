package com.onetowncity.app.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * OneTownCity's type scale: display/headline/title/body/label roles, each
 * with an explicit weight, size, line-height, and letter-spacing. All sizes
 * are `sp`, so every style already scales correctly with the system font-size
 * setting (up to 2.0x) without any extra code — see OneTownCityTheme, which
 * hands this straight to MaterialTheme with no re-scaling.
 */
object OneTownCityTypography {
    private val defaultPlatformTextStyle = PlatformTextStyle(includeFontPadding = false)

    private fun textStyle(
        fontSize: TextUnit,
        fontWeight: FontWeight = FontWeight.Normal,
        lineHeight: TextUnit = 1.4.sp,
        letterSpacing: TextUnit = 0.sp,
        textAlign: TextAlign = TextAlign.Start,
    ): TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        textAlign = textAlign,
        platformStyle = defaultPlatformTextStyle,
    )

    val default = Typography(
        displayLarge = textStyle(32.sp, FontWeight.Bold, 40.sp, (-0.02).sp),
        displayMedium = textStyle(28.sp, FontWeight.Bold, 36.sp, (-0.02).sp),
        displaySmall = textStyle(24.sp, FontWeight.Bold, 32.sp, (-0.02).sp),
        headlineLarge = textStyle(22.sp, FontWeight.Bold, 28.sp, (-0.01).sp),
        headlineMedium = textStyle(20.sp, FontWeight.SemiBold, 26.sp, (-0.01).sp),
        headlineSmall = textStyle(18.sp, FontWeight.SemiBold, 24.sp),
        titleLarge = textStyle(18.sp, FontWeight.SemiBold, 24.sp),
        titleMedium = textStyle(16.sp, FontWeight.SemiBold, 22.sp),
        titleSmall = textStyle(14.sp, FontWeight.SemiBold, 18.sp),
        bodyLarge = textStyle(16.sp, FontWeight.Normal, 24.sp),
        bodyMedium = textStyle(14.sp, FontWeight.Normal, 20.sp),
        bodySmall = textStyle(12.sp, FontWeight.Normal, 16.sp),
        labelLarge = textStyle(14.sp, FontWeight.Medium, 20.sp),
        labelMedium = textStyle(12.sp, FontWeight.Medium, 16.sp),
        labelSmall = textStyle(11.sp, FontWeight.Medium, 14.sp),
    )
}
