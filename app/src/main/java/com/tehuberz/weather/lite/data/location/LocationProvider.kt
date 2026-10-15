package com.tehuberz.weather.lite.data.location

/**
 * Abstraction over the platform location API so the ViewModel can be unit tested
 * without Android / Google Play Services dependencies.
 */
interface LocationProvider {
    /**
     * Returns the last known location as a (latitude, longitude) pair, or `null`
     * when the device has no usable coordinate (off, denied, or unavailable).
     */
    suspend fun getLastKnownCoordinates(): LocationCoordinates?
}

data class LocationCoordinates(
    val latitude: Double,
    val longitude: Double,
)
