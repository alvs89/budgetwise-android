package com.example.data.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observes validated Android internet connectivity so Firestore's initial cache emit is never
 * misreported as "Offline" when the device has active Wi-Fi or cellular data.
 *
 * All [ConnectivityManager] calls are defensively guarded against OEM / Android 11+
 * `SecurityException` quirks (e.g., AOSP b/175055271 on ColorOS/Oppo/Realme/Samsung devices)
 * so connectivity monitoring can never crash app startup or `ConnectivityThread`.
 */
object NetworkConnectivityObserver {
  private const val TAG = "NetworkConnectivity"

  fun isCurrentlyOnline(context: Context): Boolean = runCatching {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      ?: return@runCatching true
    val active = cm.activeNetwork ?: return@runCatching false
    val caps = cm.getNetworkCapabilities(active) ?: return@runCatching false
    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }.onFailure { err ->
    Log.w(TAG, "Connectivity check failed; defaulting to online", err)
  }.getOrDefault(true)

  fun observeIsOnline(context: Context): Flow<Boolean> = callbackFlow {
    val appContext = context.applicationContext ?: context
    val cm = runCatching {
      appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }.getOrNull()

    if (cm == null) {
      trySend(true)
      awaitClose { }
      return@callbackFlow
    }

    trySend(isCurrentlyOnline(appContext))

    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        runCatching { trySend(isCurrentlyOnline(appContext)) }
      }

      override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        runCatching {
          val validated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
          trySend(validated || isCurrentlyOnline(appContext))
        }
      }

      override fun onLost(network: Network) {
        runCatching { trySend(isCurrentlyOnline(appContext)) }
      }

      override fun onUnavailable() {
        runCatching { trySend(false) }
      }
    }

    val registered = runCatching {
      cm.registerDefaultNetworkCallback(callback)
      true
    }.onFailure { err ->
      Log.w(TAG, "registerDefaultNetworkCallback failed; falling back to optimistic online state", err)
      trySend(true)
    }.getOrDefault(false)

    awaitClose {
      if (registered) {
        runCatching { cm.unregisterNetworkCallback(callback) }
      }
    }
  }.distinctUntilChanged()
}
