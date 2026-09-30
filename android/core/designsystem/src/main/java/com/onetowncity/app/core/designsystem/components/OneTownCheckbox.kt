package com.onetowncity.app.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.onetowncity.app.core.designsystem.R
import com.onetowncity.app.core.designsystem.theme.OneTownTheme

/**
 * A labelled checkbox. The whole row is the touch target and is announced as one control ("checkbox, checked") by
 * TalkBack. Checked uses white, not the accent red — red is reserved for the single primary action on a screen.
 */
@Composable
fun OneTownCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = OneTownTheme.colors
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(OneTownTheme.shapes.widget)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(shape)
                .background(if (checked) colors.textPrimary else colors.background)
                .border(OneTownTheme.elevation.hairline, if (checked) colors.textPrimary else colors.outlineStrong, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Image(
                    painter = painterResource(R.drawable.ic_bi_check_lg),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(colors.background),
                )
            }
        }
        OneTownText(label, modifier = Modifier.padding(start = 16.dp), style = OneTownTheme.typography.sansBody)
    }
}
