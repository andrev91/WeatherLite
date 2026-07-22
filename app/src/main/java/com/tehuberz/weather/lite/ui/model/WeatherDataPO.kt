package com.tehuberz.weather.lite.ui.model

import com.tehuberz.weather.lite.util.UiText

data class WeatherDataPO(
    val temperatureFahrenheit : UiText,
    val temperatureCelsius : UiText,
    val weatherDescription: UiText,
    val weatherIcon: String? = null,
    val observedAt : UiText,
)
