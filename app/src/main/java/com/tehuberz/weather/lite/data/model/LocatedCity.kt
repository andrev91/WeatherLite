package com.tehuberz.weather.lite.data.model

/**
 * A city mapped back to one of the app's internal US states and city lists.
 *
 * Used by the "Use Current Location" flow to keep dropdown consistency when a
 * GPS coordinate is resolved to a place name via reverse geocoding.
 */
data class LocatedCity(
    val state: State,
    val city: String,
)
