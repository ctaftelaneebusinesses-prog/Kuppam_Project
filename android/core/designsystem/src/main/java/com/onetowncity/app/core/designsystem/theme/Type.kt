package com.onetowncity.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.onetowncity.app.core.designsystem.R

/*
 * Both files are variable fonts, so one resource serves every weight (API 26+ honours the
 * variation settings). Licences: docs/font-licenses (SIL OFL 1.1).
 *  - Inter: geometric sans for body, labels, buttons.
 *  - Doto: dot-matrix face for large numbers and spaced category headers. It replaces Nothing's
 *    proprietary Ndot-57, which we are not licensed to ship; swapping it later means replacing
 *    res/font/doto.ttf and nothing else.
 */
@OptIn(ExperimentalTextApi::class)
private fun inter(weight: FontWeight) = Font(
    resId = R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

@OptIn(ExperimentalTextApi::class)
private fun doto(weight: FontWeight) = Font(
    resId = R.font.doto,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.Setting("ROND", 0f),
    ),
)

val SansFamily = FontFamily(
    inter(FontWeight.Normal), inter(FontWeight.Medium), inter(FontWeight.SemiBold), inter(FontWeight.Bold),
)

val DotMatrixFamily = FontFamily(doto(FontWeight.Normal), doto(FontWeight.Bold))

/** Sizes are in sp on purpose: they scale with the system font size and are never shrunk to fit. */
@Immutable
class OneTownTypography(
    /** Large numbers and screen statements ("SIGNAL REQUIRED", listing counts). */
    val dotMatrixDisplay: TextStyle,
    /** Small, widely spaced category headers ("P R O P E R T Y"). */
    val dotMatrixHeader: TextStyle,
    val sansTitle: TextStyle,
    val sansBody: TextStyle,
    val sansLabel: TextStyle,
    /** Button labels — bold and 18sp, which counts as large text for contrast purposes. */
    val buttonLabel: TextStyle,
)

val DefaultOneTownTypography = OneTownTypography(
    dotMatrixDisplay = TextStyle(
        fontFamily = DotMatrixFamily, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = 0.04.em,
    ),
    dotMatrixHeader = TextStyle(
        fontFamily = DotMatrixFamily, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.4.em,
    ),
    sansTitle = TextStyle(
        fontFamily = SansFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 24.sp,
    ),
    sansBody = TextStyle(
        fontFamily = SansFamily, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp,
    ),
    sansLabel = TextStyle(
        fontFamily = SansFamily, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.em,
    ),
    buttonLabel = TextStyle(
        fontFamily = SansFamily, fontWeight = FontWeight.Bold,
        fontSize = 18.sp, lineHeight = 24.sp,
    ),
)

val LocalOneTownTypography = staticCompositionLocalOf { DefaultOneTownTypography }
