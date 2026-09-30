package com.onetowncity.app.core.data.auth

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import com.onetowncity.app.core.data.LoadFailure
import java.io.IOException

/** Where the signed-in user stands. All rules (consent, blocking) live on the server; this only reports them. */
sealed interface AccountStatus {
    data object SignedOut : AccountStatus

    /** Signed in but has not yet confirmed being 18+ and accepting the Terms. */
    data object NeedsConsent : AccountStatus
    data class Ready(val displayName: String) : AccountStatus
    data object Blocked : AccountStatus
    data class Failed(val reason: LoadFailure) : AccountStatus
}

interface AccountRepository {
    suspend fun status(): AccountStatus

    /** Sends the two consent confirmations. Returns the new status; the server validates that both are true. */
    suspend fun confirmConsent(): AccountStatus
    fun signOut()
}

@Serializable
internal data class MeDto(
    @SerialName("full_name") val fullName: String = "",
    val username: String = "",
    @SerialName("consent_confirmed") val consentConfirmed: Boolean = false,
)

@Serializable
internal data class ConsentBody(
    @SerialName("confirm_adult") val confirmAdult: Boolean,
    @SerialName("accept_terms") val acceptTerms: Boolean,
)

internal interface AccountApi {
    @GET("api/v1/auth/me/")
    suspend fun me(): MeDto

    @POST("api/v1/auth/age-confirmation/")
    suspend fun confirmConsent(@Body body: ConsentBody)
}

internal class NetworkAccountRepository(
    private val api: AccountApi,
    private val auth: SupabaseAuth,
) : AccountRepository {

    override suspend fun status(): AccountStatus {
        if (!auth.hasSession()) return AccountStatus.SignedOut
        return call { statusFromMe() }
    }

    override suspend fun confirmConsent(): AccountStatus = call {
        api.confirmConsent(ConsentBody(confirmAdult = true, acceptTerms = true))
        statusFromMe()
    }

    override fun signOut() = auth.signOut()

    private suspend fun statusFromMe(): AccountStatus {
        val me = api.me()
        return if (me.consentConfirmed) AccountStatus.Ready(me.fullName.ifBlank { me.username })
        else AccountStatus.NeedsConsent
    }

    private suspend fun call(block: suspend () -> AccountStatus): AccountStatus = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Log.w(TAG, "Account request failed: HTTP ${e.code()}")
        when (e.code()) {
            401 -> {
                // The server rejected the token (revoked, expired beyond refresh, or the account was blocked).
                auth.signOut()
                AccountStatus.SignedOut
            }
            403 -> AccountStatus.Blocked
            else -> AccountStatus.Failed(LoadFailure.Server)
        }
    } catch (e: SerializationException) {
        Log.w(TAG, "Account response could not be read: ${e.javaClass.simpleName}")
        AccountStatus.Failed(LoadFailure.Malformed)
    } catch (e: IOException) {
        Log.w(TAG, "Account request could not reach the server: ${e.javaClass.simpleName}")
        AccountStatus.Failed(LoadFailure.Network)
    }

    private companion object {
        const val TAG = "OneTownAccount"
    }
}
