package com.onetowncity.app.feature.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onetowncity.app.core.designsystem.components.DotMatrixText
import com.onetowncity.app.core.designsystem.components.OneTownPrimaryButton
import com.onetowncity.app.core.designsystem.components.OneTownSecondaryButton
import com.onetowncity.app.core.designsystem.components.OneTownText
import com.onetowncity.app.core.designsystem.theme.OneTownTheme

/**
 * Onboarding step 2 (after the splash): explains *why* location is needed, and only then triggers the system
 * permission dialog. Declining is a normal outcome — the user can pick a city manually — so the app never dead-ends here.
 */
@Composable
fun LocationOnboarding(
    onFinished: (LocationOutcome) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationOnboardingViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val latestOnFinished by rememberUpdatedState(onFinished)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        viewModel.onPermissionResult(granted = result.values.any { it })
    }
    LaunchedEffect(viewModel) { viewModel.outcomes.collect { latestOnFinished(it) } }

    LocationOnboardingContent(
        stage = state.stage,
        onEnableLocation = {
            launcher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        },
        onChooseManually = viewModel::onChooseManually,
        onOpenSettings = { openAppSettings(context) },
        modifier = modifier,
    )
}

/** Stateless UI for previews and tests. */
@Composable
fun LocationOnboardingContent(
    stage: LocationStage,
    onEnableLocation: () -> Unit,
    onChooseManually: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OneTownTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp).padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DotMatrixCompass(Modifier.fillMaxWidth(0.8f))
            Spacer(Modifier.height(8.dp))
            DotMatrixText(
                text = stringResource(R.string.location_title).uppercase(),
                textAlign = TextAlign.Center,
            )
            OneTownText(
                text = stringResource(R.string.location_body),
                style = OneTownTheme.typography.sansBody,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            if (stage == LocationStage.Denied) {
                OneTownText(
                    text = stringResource(R.string.location_denied),
                    style = OneTownTheme.typography.sansLabel,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(8.dp))
            OneTownPrimaryButton(stringResource(R.string.location_enable), onEnableLocation)
            OneTownSecondaryButton(stringResource(R.string.location_manual), onChooseManually)
            if (stage == LocationStage.Denied) {
                OneTownSecondaryButton(stringResource(R.string.location_open_settings), onOpenSettings)
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
