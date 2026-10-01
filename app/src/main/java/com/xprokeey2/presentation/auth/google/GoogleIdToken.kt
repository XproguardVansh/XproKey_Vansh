package com.xprokeey2.presentation.auth.google

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.xprokeey2.BuildConfig

/** What Google's account picker gave back. */
sealed interface GoogleIdTokenResult {
    data class Success(val idToken: String) : GoogleIdTokenResult {
        override fun toString(): String = "Success(idToken=…)"
    }

    /** The user closed the picker: nothing to say, like closing the web's Google pop-up. */
    data object Cancelled : GoogleIdTokenResult

    data object Failed : GoogleIdTokenResult
}

/**
 * Shows Google's "Sign in with Google" sheet (Credential Manager) and returns the chosen account's ID
 * token. It is issued for the web app's client ID, like the web's Google button, so POST /auth/google
 * checks it the same way. [activityContext] must be an Activity: the sheet opens over it.
 */
suspend fun requestGoogleIdToken(activityContext: Context): GoogleIdTokenResult {
    val option = GetSignInWithGoogleOption.Builder(serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    return try {
        val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            GoogleIdTokenResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            Log.w(TAG, "Unexpected credential type: ${credential.type}")
            GoogleIdTokenResult.Failed
        }
    } catch (e: GetCredentialCancellationException) {
        // Also what Google reports when this app isn't registered for the client ID ("Invalid key
        // value" in the Auth log), so it is logged; the user just sees the sheet close.
        Log.i(TAG, "Google sign-in cancelled: ${e.message}")
        GoogleIdTokenResult.Cancelled
    } catch (e: GetCredentialException) {
        // e.g. "Developer console is not set up correctly" when Google Cloud has no Android client
        // for this package and signing certificate. No token is in the message.
        Log.w(TAG, "Google sign-in failed: ${e.type}: ${e.message}")
        GoogleIdTokenResult.Failed
    } catch (e: GoogleIdTokenParsingException) {
        Log.w(TAG, "Google ID token couldn't be read", e)
        GoogleIdTokenResult.Failed
    }
}

private const val TAG = "GoogleSignIn"
