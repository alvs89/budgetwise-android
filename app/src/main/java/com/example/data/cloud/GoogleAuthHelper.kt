package com.example.data.cloud

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface GoogleSignInResult {
  data class Success(val user: FirebaseUser) : GoogleSignInResult
  data object Cancelled : GoogleSignInResult
  data class Failure(val message: String) : GoogleSignInResult
}

/**
 * Resolves BudgetWise's provisioned named Firestore database instance.
 */
fun getBudgetWiseFirestore(context: Context): FirebaseFirestore {
  val databaseId = context.getString(R.string.firestore_database_id)
  return FirebaseFirestore.getInstance(databaseId)
}

/**
 * Reactive stream of the currently authenticated [FirebaseUser], emitting null when signed out.
 */
fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
    trySend(firebaseAuth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

object GoogleAuthHelper {
  private const val TAG = "Auth"

  /**
   * Silent startup auto sign-in using ONLY [GetGoogleIdOption] for previously authorized accounts.
   */
  suspend fun attemptAutoSignIn(
    activityContext: Context,
    auth: FirebaseAuth,
  ): FirebaseUser? {
    if (auth.currentUser != null) return auth.currentUser
    val webClientId = activityContext.getString(R.string.default_web_client_id)
    if (webClientId.isBlank()) return null

    val credentialManager = CredentialManager.create(activityContext)
    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setAutoSelectEnabled(true)
      .setServerClientId(webClientId)
      .build()

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    return try {
      val result = credentialManager.getCredential(
        context = activityContext,
        request = request,
      )
      exchangeGoogleCredential(result.credential, auth)
    } catch (e: CancellationException) {
      throw e
    } catch (_: NoCredentialException) {
      null
    } catch (_: GetCredentialCancellationException) {
      null
    } catch (e: Exception) {
      Log.w(TAG, "Silent Google auto sign-in skipped: ${e.message}")
      null
    }
  }

  /**
   * Interactive Google Sign-In button flow using ONLY [GetSignInWithGoogleOption] (single option
   * per request for Android 16 / API 36 Credential Manager compatibility).
   */
  suspend fun onGoogleSignInClicked(
    activityContext: Context,
    auth: FirebaseAuth,
  ): GoogleSignInResult {
    val webClientId = activityContext.getString(R.string.default_web_client_id)
    if (webClientId.isBlank()) {
      return GoogleSignInResult.Failure("Google Sign-In configuration is unavailable.")
    }

    val credentialManager = CredentialManager.create(activityContext)
    val signInOption = GetSignInWithGoogleOption.Builder(webClientId)
      .build()

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    return try {
      val response = credentialManager.getCredential(
        context = activityContext,
        request = request,
      )
      val user = exchangeGoogleCredential(response.credential, auth)
      if (user != null) {
        GoogleSignInResult.Success(user)
      } else {
        GoogleSignInResult.Failure("Could not complete Google Sign-In. Please try again.")
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: GetCredentialCancellationException) {
      Log.w(TAG, "Google sign-in cancelled by user", e)
      GoogleSignInResult.Cancelled
    } catch (e: GetCredentialException) {
      Log.e(TAG, "Credential Manager sign-in error: ${e.type} - ${e.message}", e)
      GoogleSignInResult.Failure(
        e.errorMessage?.toString()?.takeIf { it.isNotBlank() }
          ?: "Unable to sign in with Google. Check your network and Google account."
      )
    } catch (e: GoogleIdTokenParsingException) {
      Log.e(TAG, "Failed to parse Google ID token", e)
      GoogleSignInResult.Failure("Received an invalid Google credential. Please try again.")
    } catch (e: Exception) {
      Log.e(TAG, "Firebase Google authentication failed", e)
      GoogleSignInResult.Failure(
        e.localizedMessage?.takeIf { it.isNotBlank() }
          ?: "Authentication failed. Please try again."
      )
    }
  }

  /**
   * Signs out of Firebase Auth and clears Credential Manager state inside [NonCancellable].
   */
  suspend fun signOut(
    context: Context,
    auth: FirebaseAuth,
    onBeforeSignOut: suspend () -> Unit = {},
  ) {
    withContext(NonCancellable) {
      runCatching { onBeforeSignOut() }
        .onFailure { Log.w(TAG, "Pre-sign-out cleanup warning: ${it.message}") }
      runCatching {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
      }.onFailure { Log.w(TAG, "Credential Manager state clear warning: ${it.message}") }
      auth.signOut()
    }
  }

  private suspend fun exchangeGoogleCredential(
    credential: androidx.credentials.Credential,
    auth: FirebaseAuth,
  ): FirebaseUser? {
    if (credential is CustomCredential &&
      credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
      val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
      val firebaseCredential = GoogleAuthProvider.getCredential(
        googleIdTokenCredential.idToken,
        null,
      )
      return auth.signInWithCredential(firebaseCredential).await().user
    }
    return null
  }
}
