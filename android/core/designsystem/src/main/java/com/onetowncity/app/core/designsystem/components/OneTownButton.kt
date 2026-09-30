package com.onetowncity.app.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onetowncity.app.core.designsystem.theme.OneTownTheme

/** Full-width red pill. Reserved for the one action that matters on a screen (e.g. Enable location). */
@Composable
fun OneTownPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = OneTownTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinTouchTarget)
            .clip(OneTownTheme.shapes.pill)
            .background(if (enabled) colors.accent else colors.surfaceRaised)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        OneTownText(
            text,
            style = OneTownTheme.typography.buttonLabel,
            color = if (enabled) colors.onAccent else colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

/** Outlined pill for secondary actions. */
@Composable
fun OneTownSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OneTownTheme.colors
    val shape = OneTownTheme.shapes.pill
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinTouchTarget)
            .clip(shape)
            .border(OneTownTheme.elevation.hairline, colors.outlineStrong, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        OneTownText(text, style = OneTownTheme.typography.buttonLabel, textAlign = TextAlign.Center)
    }
}

/** 56.dp — comfortably above the 48.dp accessibility minimum. */
private val MinTouchTarget = 56.dp

/** Quiet text-only action (e.g. Sign out, Read the Terms). Still a full 48.dp touch target. */
@Composable
fun OneTownTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
            .clip(OneTownTheme.shapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        OneTownText(text, style = OneTownTheme.typography.sansLabel, color = OneTownTheme.colors.textSecondary)
    }
}

/** Icon-only button with a mandatory spoken label (icon buttons without one are invisible to TalkBack). */
@Composable
fun OneTownIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
            .clip(OneTownTheme.shapes.pill)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(OneTownTheme.colors.textPrimary),
        )
    }
}
