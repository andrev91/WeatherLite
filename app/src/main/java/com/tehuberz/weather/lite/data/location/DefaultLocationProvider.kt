package com.tehuberz.weather.lite.data.location

import android.annotation.SuppressLint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

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
        val last = fusedLocationProviderClient.lastLocation.await()
        if (last != null) return LocationCoordinates(last.latitude, last.longitude)

        val current =
            fusedLocationProviderClient
                .getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token,
                ).await()
        return current?.let { LocationCoordinates(it.latitude, it.longitude) }
    }
}
