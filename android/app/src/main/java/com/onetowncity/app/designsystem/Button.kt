package com.onetowncity.app.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

object OneTownCityButtonDefaults {
    val minHeight = OneTownCityTouchTarget.minSize
}

enum class OneTownCityButtonVariant {
    Primary,
    Secondary,
    Outlined,
    Text,
}

@Composable
fun OneTownCityButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    variant: OneTownCityButtonVariant = OneTownCityButtonVariant.Primary,
) {
    val colors = when (variant) {
        OneTownCityButtonVariant.Primary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.66f),
        )
        OneTownCityButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.66f),
        )
        OneTownCityButtonVariant.Outlined -> ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
        OneTownCityButtonVariant.Text -> ButtonDefaults.textButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }

    val shape = RoundedCornerShape(OneTownCityCornerRadii.md)
    val adaptiveModifier = modifier.defaultMinSize(minHeight = OneTownCityButtonDefaults.minHeight)

    when (variant) {
        OneTownCityButtonVariant.Outlined -> {
            OutlinedButton(
                onClick = onClick,
                modifier = adaptiveModifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                ButtonContent(text, leadingIcon, trailingIcon)
            }
        }
        OneTownCityButtonVariant.Text -> {
            TextButton(
                onClick = onClick,
                modifier = adaptiveModifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                ButtonContent(text, leadingIcon, trailingIcon)
            }
        }
        else -> {
            Button(
                onClick = onClick,
                modifier = adaptiveModifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                ButtonContent(text, leadingIcon, trailingIcon)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OneTownCitySpacing.xs),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge,
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
