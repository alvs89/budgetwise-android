package com.example.data.cloud

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write"),
}

/**
 * Logs structured diagnostic JSON for Firestore failures without swallowing coroutine cancellation.
 */
object FirestoreErrorHandler {
  private const val TAG = "FirestoreError"

  fun logFirestoreError(
    error: Throwable,
    operationType: OperationType,
    path: String?,
  ): String {
    if (error is CancellationException) throw error

    val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
    val providerArray = JSONArray()
    currentUser?.providerData?.forEach { info ->
      providerArray.put(
        JSONObject().apply {
          put("providerId", info.providerId)
          put("displayName", info.displayName ?: JSONObject.NULL)
          put("email", info.email ?: JSONObject.NULL)
        }
      )
    }

    val payload = JSONObject().apply {
      put("error", error.message ?: error.javaClass.simpleName)
      put("operationType", operationType.value)
      put("path", path ?: JSONObject.NULL)
      put(
        "authInfo",
        JSONObject().apply {
          put("userId", currentUser?.uid ?: JSONObject.NULL)
          put("email", currentUser?.email ?: JSONObject.NULL)
          put("emailVerified", currentUser?.isEmailVerified ?: false)
          put("isAnonymous", currentUser?.isAnonymous ?: false)
          put("providerInfo", providerArray)
        }
      )
    }.toString()

    Log.e(TAG, payload, error)
    return payload
  }

  fun handleFirestoreError(
    error: Throwable,
    operationType: OperationType,
    path: String?,
  ): Nothing {
    val payload = logFirestoreError(error, operationType, path)
    throw IllegalStateException(payload, error)
  }
}
