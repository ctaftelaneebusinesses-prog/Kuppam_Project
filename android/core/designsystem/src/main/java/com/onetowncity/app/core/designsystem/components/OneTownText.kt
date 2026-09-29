package com.onetowncity.app.core.designsystem.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.onetowncity.app.core.designsystem.theme.OneTownTheme
import com.onetowncity.app.core.designsystem.theme.SansFamily

/** Text with design-system defaults (there is no Material `Text` / `LocalContentColor` here). */
@Composable
fun OneTownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = OneTownTheme.typography.sansBody,
    color: Color = OneTownTheme.colors.textPrimary,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = textAlign ?: style.textAlign),
        overflow = overflow,
        maxLines = maxLines,
    )
}

/**
 * Dot-matrix statement or number. The dot-matrix face only covers Latin glyphs, so any other
 * script (Telugu, Hindi, …) falls back to the sans family at the same size instead of showing
 * missing-glyph boxes.
 */
@Composable
fun DotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = OneTownTheme.typography.dotMatrixDisplay,
    color: Color = OneTownTheme.colors.textPrimary,
    textAlign: TextAlign? = null,
) {
    val latin = remember(text) { text.all { it.code < LatinExtendedLimit } }
    val effective = if (latin) style else style.copy(
        fontFamily = SansFamily,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = TextUnit.Unspecified,
    )
    OneTownText(text, modifier, effective, color, textAlign)
}

/**
 * Widely spaced category header ("P R O P E R T Y"). The spacing is typographic
 * (`letterSpacing`), never literal spaces, and screen readers announce the real word as a heading.
 */
@Composable
fun DotMatrixHeader(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = OneTownTheme.colors.textSecondary,
) {
    DotMatrixText(
        text = text.uppercase(),
        style = OneTownTheme.typography.dotMatrixHeader,
        color = color,
        modifier = modifier.clearAndSetSemantics {
            contentDescription = text
            heading()
        },
    )
}

private const val LatinExtendedLimit = 0x250
