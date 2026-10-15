package com.tehuberz.weather.lite.ui.state

import com.tehuberz.weather.lite.data.model.TemperatureUnit
import com.tehuberz.weather.lite.ui.model.WeatherDataPO

data class WeatherDataState(
    val isLoadingWeather: Boolean = false,
    val weatherContent: WeatherDataPO? = null,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
)
