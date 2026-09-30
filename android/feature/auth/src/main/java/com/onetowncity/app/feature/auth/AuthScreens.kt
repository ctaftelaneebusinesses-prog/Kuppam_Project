package com.onetowncity.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onetowncity.app.core.designsystem.components.DotMatrixText
import com.onetowncity.app.core.designsystem.components.OneTownCheckbox
import com.onetowncity.app.core.designsystem.components.OneTownPrimaryButton
import com.onetowncity.app.core.designsystem.components.OneTownSecondaryButton
import com.onetowncity.app.core.designsystem.components.OneTownText
import com.onetowncity.app.core.designsystem.components.OneTownTextButton
import com.onetowncity.app.core.designsystem.theme.OneTownTheme

/** Why the last sign-in attempt did not finish; each maps to one plain-language message. */
enum class SignInError { Cancelled, Rejected, Network, NotConfigured }

/** Shared frame: true-black page, safe-area padding, a centred column no wider than 480.dp, scrollable at any font size. */
@Composable
private fun AuthFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OneTownTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp).padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) { content() }
    }
}

@Composable
fun SignInScreen(
    busy: Boolean,
    error: SignInError?,
    onContinueWithGoogle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthFrame(modifier) {
        DotMatrixText(stringResource(R.string.sign_in_title).uppercase(), textAlign = TextAlign.Center)
        OneTownText(
            stringResource(R.string.sign_in_body),
            color = OneTownTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        if (error != null) {
            OneTownText(
                text = stringResource(error.messageRes()),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = OneTownTheme.typography.sansLabel,
                textAlign = TextAlign.Center,
            )
        }
        if (busy) {
            OneTownText(
                text = stringResource(R.string.sign_in_working),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = OneTownTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        } else {
            // Red is kept for the one primary action; sign-in is that action on this screen.
            OneTownPrimaryButton(stringResource(R.string.sign_in_google), onContinueWithGoogle)
        }
    }
}

private fun SignInError.messageRes() = when (this) {
    SignInError.Cancelled -> R.string.sign_in_error_cancelled
    SignInError.Rejected -> R.string.sign_in_error_rejected
    SignInError.Network -> R.string.sign_in_error_network
    SignInError.NotConfigured -> R.string.sign_in_error_not_configured
}

/**
 * The one-time 18+ / Terms confirmation. Continue stays disabled until both boxes are ticked; the server re-checks
 * both, so this is a convenience and never the only enforcement.
 */
@Composable
fun ConsentScreen(
    busy: Boolean,
    saveFailed: Boolean,
    onConfirm: () -> Unit,
    onReadTerms: () -> Unit,
    onReadPrivacy: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var adult by rememberSaveable { mutableStateOf(false) }
    var terms by rememberSaveable { mutableStateOf(false) }
    AuthFrame(modifier) {
        DotMatrixText(stringResource(R.string.consent_title).uppercase(), textAlign = TextAlign.Center)
        OneTownText(
            stringResource(R.string.consent_body),
            color = OneTownTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        OneTownCheckbox(adult, { adult = it }, stringResource(R.string.consent_adult))
        OneTownCheckbox(terms, { terms = it }, stringResource(R.string.consent_terms))
        OneTownTextButton(stringResource(R.string.consent_read_terms), onReadTerms)
        OneTownTextButton(stringResource(R.string.consent_read_privacy), onReadPrivacy)
        if (saveFailed) {
            OneTownText(
                text = stringResource(R.string.consent_error),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = OneTownTheme.typography.sansLabel,
                textAlign = TextAlign.Center,
            )
        }
        OneTownPrimaryButton(
            text = stringResource(R.string.consent_continue),
            onClick = onConfirm,
            enabled = adult && terms && !busy,
        )
        OneTownTextButton(stringResource(R.string.consent_sign_out), onSignOut)
    }
}

@Composable
fun BlockedScreen(onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    AuthFrame(modifier) {
        DotMatrixText(stringResource(R.string.blocked_title).uppercase(), textAlign = TextAlign.Center)
        OneTownText(
            stringResource(R.string.blocked_body),
            color = OneTownTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        OneTownSecondaryButton(stringResource(R.string.consent_sign_out), onSignOut)
    }
}

@Composable
fun ProblemScreen(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    AuthFrame(modifier) {
        DotMatrixText(stringResource(R.string.problem_title).uppercase(), textAlign = TextAlign.Center)
        OneTownText(
            stringResource(R.string.problem_body),
            color = OneTownTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        OneTownSecondaryButton(stringResource(R.string.problem_retry), onRetry)
    }
}
