package com.onetowncity.app.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun OneTownCityDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmText: String = "OK",
    dismissText: String? = null,
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        Surface(
            shape = RoundedCornerShape(OneTownCityCornerRadii.xl),
            tonalElevation = OneTownCityElevation.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            Column(
                modifier = Modifier.padding(OneTownCitySpacing.xl),
                verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.md),
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dismissText != null) {
                        TextButton(
                            onClick = {
                                onDismiss?.invoke()
                                onDismissRequest()
                            },
                        ) {
                            Text(dismissText)
                        }
                    }
                    Spacer(modifier = Modifier.width(OneTownCitySpacing.sm))
                    OneTownCityButton(
                        text = confirmText,
                        onClick = {
                            onConfirm?.invoke()
                            onDismissRequest()
                        },
                        variant = OneTownCityButtonVariant.Primary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneTownCityBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = OneTownCityCornerRadii.xl, topEnd = OneTownCityCornerRadii.xl),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        content()
    }
}
