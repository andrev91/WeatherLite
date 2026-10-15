package com.tehuberz.weather.lite.data.location

import android.annotation.SuppressLint
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * [LocationProvider] backed by Google Play Services'
 * [FusedLocationProviderClient]. Bound to the [LocationProvider] interface by
 * [com.tehuberz.weather.lite.di.LocationModule].
 */
@SuppressLint("MissingPermission")
class DefaultLocationProvider(
    private val fusedLocationProviderClient: FusedLocationProviderClient,
) : LocationProvider {
    override suspend fun getLastKnownCoordinates(): LocationCoordinates? {
        try {
            val last = fusedLocationProviderClient.lastLocation.await()
            if (last != null) return LocationCoordinates(last.latitude, last.longitude)
        } catch (e: SecurityException) {
            return handleSecurityException("Location permission missing or revoked while fetching last location", e)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to get last known location from fused client", e)
        }

        val cancellationTokenSource = CancellationTokenSource()
        return try {
            withTimeoutOrNull(LOCATION_TIMEOUT) {
                val current =
                    fusedLocationProviderClient
                        .getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            cancellationTokenSource.token,
                        ).await()
                current?.let { LocationCoordinates(it.latitude, it.longitude) }
            }
        } catch (e: SecurityException) {
            handleSecurityException("Location permission missing or revoked while requesting current location", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current location from fused client", e)
            null
        } finally {
            cancellationTokenSource.cancel()
        }
    }

    private fun handleSecurityException(
        message: String,
        e: SecurityException,
    ): Nothing? {
        Log.e(TAG, message, e)
        return null
    }

    companion object {
        private const val TAG = "DefaultLocationProvider"
        private val LOCATION_TIMEOUT = 10.seconds
    }
}
