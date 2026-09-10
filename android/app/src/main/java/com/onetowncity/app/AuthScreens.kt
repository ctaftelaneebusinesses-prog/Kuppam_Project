package com.onetowncity.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.core.net.toUri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.onetowncity.app.auth.AuthState
import com.onetowncity.app.auth.SessionManager
import com.onetowncity.app.designsystem.OneTownCityButton
import com.onetowncity.app.designsystem.OneTownCityButtonVariant
import com.onetowncity.app.designsystem.OneTownCityCard
import com.onetowncity.app.designsystem.OneTownCityCircularLoading
import com.onetowncity.app.designsystem.OneTownCityErrorState
import com.onetowncity.app.designsystem.OneTownCityTopAppBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Opens Supabase's hosted Google sign-in page (the same provider config
 * templates/signin.html uses via supabase.auth.signInWithOAuth) in a Custom
 * Tab. Android never implements the Google OAuth dance itself.
 */
private fun launchGoogleSignIn(context: Context) {
    val uri = SessionManager.beginSignIn()
    val customTabsIntent = CustomTabsIntent.Builder().build()
    try {
        customTabsIntent.launchUrl(context, uri)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) { }
    }
}

/**
 * Opens a real OneTownCity web page (Privacy Policy, Terms of Service) in a
 * Custom Tab — same fallback-to-plain-browser pattern as launchGoogleSignIn.
 * Android never re-hosts this content locally; it always reflects whatever
 * the web app currently serves at that URL.
 */
private fun openWebPage(context: Context, path: String) {
    val uri = "$API_BASE_URL$path".toUri()
    val customTabsIntent = CustomTabsIntent.Builder().build()
    try {
        customTabsIntent.launchUrl(context, uri)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) { }
    }
}

/**
 * DELETE /api/v1/auth/me/ (core.api.views.me's DELETE method — the exact
 * same backend service the web "Delete My Account" page calls; see
 * core.account_deletion.delete_user_account). No deletion logic lives here
 * — this only calls the real endpoint with the same {"confirm": "DELETE"}
 * contract the web flow requires, and reports whatever the server does.
 */
private suspend fun deleteMyAccount() {
    httpJson(
        "$API_BASE_URL/api/v1/auth/me/",
        method = "DELETE",
        jsonBody = """{"confirm":"DELETE"}""",
        requiresAuth = true,
    )
}

@Composable
internal fun SignInScreen(navController: NavController) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        OneTownCityTopAppBar(
            title = "Sign in",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = { navController.popBackStack() },
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Sign in to OneTownCity",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Use the same Google account you'd use on the OneTownCity website to save favorites, write reviews, and post comments.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OneTownCityButton(
                text = "Continue with Google",
                onClick = { launchGoogleSignIn(context) },
                variant = OneTownCityButtonVariant.Primary,
            )
        }
    }
}

private sealed class AuthCallbackUiState {
    object Loading : AuthCallbackUiState()
    object Success : AuthCallbackUiState()
    data class Error(val message: String) : AuthCallbackUiState()
}

/**
 * Destination for the onetowncity://auth-callback deep link the Custom Tab
 * redirects back to once Supabase finishes the Google OAuth exchange.
 * MainActivity is singleTask and forwards the redirect intent here via
 * onNewIntent -> setIntent, so the authorization `code` is read straight off
 * the current Activity intent rather than through NavGraph argument parsing
 * (query-string deep link argument matching is brittle when Supabase may
 * also redirect with `error`/`error_description` instead of `code`).
 */
@Composable
internal fun AuthCallbackScreen(navController: NavController) {
    val activity = LocalContext.current as? Activity
    var state by remember { mutableStateOf<AuthCallbackUiState>(AuthCallbackUiState.Loading) }
    val redirectUri = activity?.intent?.data

    LaunchedEffect(redirectUri) {
        val uri = redirectUri
        val errorDescription = uri?.getQueryParameter("error_description") ?: uri?.getQueryParameter("error")
        val code = uri?.getQueryParameter("code")
        state = when {
            uri == null -> AuthCallbackUiState.Error("We couldn't complete sign-in. Please try again.")
            errorDescription != null -> AuthCallbackUiState.Error(errorDescription)
            code != null -> {
                SessionManager.completeSignIn(code).fold(
                    onSuccess = { AuthCallbackUiState.Success },
                    onFailure = { AuthCallbackUiState.Error(it.message ?: "Sign-in failed. Please try again.") },
                )
            }
            else -> AuthCallbackUiState.Error("We couldn't complete sign-in. Please try again.")
        }
        if (state is AuthCallbackUiState.Success) {
            // Return to whatever screen prompted sign-in: pop everything up
            // through "sign-in" if it's on the stack (the usual path — a
            // "Sign in" prompt pushed it), otherwise just pop this screen
            // (Profile's inline sign-in never pushed "sign-in" at all).
            if (!navController.popBackStack(route = "sign-in", inclusive = true)) {
                navController.popBackStack()
            }
        }
    }

    when (val current = state) {
        is AuthCallbackUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                OneTownCityCircularLoading(label = "Finishing sign-in…")
            }
        }
        is AuthCallbackUiState.Success -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                OneTownCityCircularLoading(label = "Signed in")
            }
        }
        is AuthCallbackUiState.Error -> {
            OneTownCityErrorState(
                title = "Sign-in failed",
                message = current.message,
                actionText = "Back",
                onRetry = { navController.popBackStack() },
            )
        }
    }
}

internal data class MeProfile(
    val username: String,
    val email: String,
    val role: String,
    val fullName: String,
    val phoneNumber: String,
    val displayPhoto: String,
    val address: String,
    val city: String,
    val state: String,
    val pincode: String,
    val profileCompleted: Boolean,
    val isSuspended: Boolean,
)

private fun parseMeProfile(json: JSONObject) = MeProfile(
    username = json.optString("username", ""),
    email = json.optString("email", ""),
    role = json.optString("role", ""),
    fullName = json.optString("full_name", ""),
    phoneNumber = json.optString("phone_number", ""),
    displayPhoto = json.optString("display_photo", ""),
    address = json.optString("address", ""),
    city = json.optString("city", ""),
    state = json.optString("state", ""),
    pincode = json.optString("pincode", ""),
    profileCompleted = json.optBoolean("profile_completed", false),
    isSuspended = json.optBoolean("is_suspended", false),
)

/** GET /api/v1/auth/me/ (core.api.views.me / MeSerializer) — the caller's own profile only. */
private suspend fun fetchMyProfile(): MeProfile =
    parseMeProfile(httpJson("$API_BASE_URL/api/v1/auth/me/", requiresAuth = true))

@Composable
internal fun ProfileScreen() {
    val authState by SessionManager.authState.collectAsState()
    val context = LocalContext.current
    var profile by remember { mutableStateOf<MeProfile?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun loadProfile() {
        coroutineScope.launch {
            isLoading = true
            error = null
            try {
                profile = fetchMyProfile()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthRequiredException) {
                profile = null
            } catch (e: Exception) {
                error = e.message ?: "Unable to load your profile right now."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.SignedIn) {
            loadProfile()
        } else {
            profile = null
            error = null
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Profile", style = MaterialTheme.typography.headlineSmall)

        val signedInState = authState as? AuthState.SignedIn
        when {
            signedInState == null -> {
                OneTownCityCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Sign in to OneTownCity", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Sign in to manage your profile, save favorites, and write reviews and comments.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OneTownCityButton(
                            text = "Continue with Google",
                            onClick = { launchGoogleSignIn(context) },
                            variant = OneTownCityButtonVariant.Primary,
                        )
                    }
                }
            }
            isLoading && profile == null -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    OneTownCityCircularLoading(label = "Loading profile")
                }
            }
            error != null && profile == null -> {
                OneTownCityErrorState(
                    title = "Unable to load your profile",
                    message = error ?: "Please try again later.",
                    actionText = "Retry",
                    onRetry = { loadProfile() },
                )
            }
            profile != null -> {
                ProfileDetails(
                    profile = profile!!,
                    fallbackName = signedInState.fullName,
                    fallbackEmail = signedInState.email,
                    fallbackAvatarUrl = signedInState.avatarUrl,
                    onSignOut = {
                        coroutineScope.launch { SessionManager.signOut() }
                    },
                )
                DeleteAccountSection(coroutineScope = coroutineScope)
            }
        }

        // Privacy Policy / Terms / Support must be reachable whether or not
        // the visitor is signed in — unlike account deletion, none of these
        // require an account, so they live outside the `when` above instead
        // of only inside the profile != null branch.
        LegalLinksSection(context = context)
    }
}

/**
 * Privacy Policy / Terms of Service / Support — reachable regardless of
 * sign-in state (see ProfileScreen). None of these require an account.
 */
@Composable
private fun LegalLinksSection(context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        OneTownCityButton(
            text = "Privacy Policy",
            onClick = { openWebPage(context, "/privacy-policy/") },
            variant = OneTownCityButtonVariant.Text,
        )
        OneTownCityButton(
            text = "Terms of Service",
            onClick = { openWebPage(context, "/terms-of-service/") },
            variant = OneTownCityButtonVariant.Text,
        )
        OneTownCityButton(
            text = "Support",
            onClick = { openWebPage(context, "/contact/") },
            variant = OneTownCityButtonVariant.Text,
        )
    }
}

/** Delete My Account — signed-in only (see ProfileScreen's profile != null branch). */
@Composable
private fun DeleteAccountSection(coroutineScope: kotlinx.coroutines.CoroutineScope) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        if (deleteError != null) {
            Text(
                text = deleteError ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        OneTownCityButton(
            text = if (isDeleting) "Deleting account..." else "Delete My Account",
            onClick = { deleteError = null; showDeleteConfirm = true },
            variant = OneTownCityButtonVariant.Outlined,
            enabled = !isDeleting,
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirm = false },
            title = { Text("Delete your account?") },
            text = {
                Text(
                    "This permanently deletes your OneTownCity account, profile, favorites, " +
                        "and notifications. Comments and reviews you posted stay visible but are " +
                        "no longer linked to you. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = {
                        coroutineScope.launch {
                            isDeleting = true
                            deleteError = null
                            try {
                                deleteMyAccount()
                                showDeleteConfirm = false
                                SessionManager.signOut()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: AuthRequiredException) {
                                deleteError = "Your session expired. Please sign in again and retry."
                                showDeleteConfirm = false
                            } catch (e: java.io.IOException) {
                                // Genuine network failure (no connection, DNS, timeout) — distinct
                                // from a real server-returned error, which httpJson surfaces as
                                // IllegalStateException below with the server's own message.
                                deleteError = "Couldn't reach OneTownCity. Check your connection and try again."
                                showDeleteConfirm = false
                            } catch (e: Exception) {
                                deleteError = e.message ?: "Unable to delete your account right now. Please try again."
                                showDeleteConfirm = false
                            } finally {
                                isDeleting = false
                            }
                        }
                    },
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !isDeleting, onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun ProfileDetails(
    profile: MeProfile,
    fallbackName: String,
    fallbackEmail: String,
    fallbackAvatarUrl: String,
    onSignOut: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val avatarUrl = profile.displayPhoto.ifBlank { fallbackAvatarUrl }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f), modifier = Modifier.size(64.dp)) {
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(model = rememberOptimizedImageRequest(avatarUrl), contentDescription = null, modifier = Modifier.size(64.dp))
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                        Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Column {
                Text(
                    text = profile.fullName.ifBlank { fallbackName }.ifBlank { profile.username },
                    style = MaterialTheme.typography.titleLarge,
                )
                if (profile.email.ifBlank { fallbackEmail }.isNotBlank()) {
                    Text(
                        text = profile.email.ifBlank { fallbackEmail },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (profile.isSuspended) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Your account is suspended from creating or managing listings.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        OneTownCityCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (profile.phoneNumber.isNotBlank()) DetailRow(icon = Icons.Filled.Call, title = profile.phoneNumber)
                val locationLine = listOf(profile.address, profile.city, profile.state, profile.pincode)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                if (locationLine.isNotBlank()) DetailRow(icon = Icons.Filled.LocationOn, title = locationLine)
                if (profile.role.isNotBlank()) DetailRow(icon = Icons.Filled.Person, title = profile.role.replaceFirstChar { it.titlecase() })
            }
        }

        OneTownCityButton(
            text = "Log out",
            onClick = onSignOut,
            variant = OneTownCityButtonVariant.Outlined,
        )
    }
}
