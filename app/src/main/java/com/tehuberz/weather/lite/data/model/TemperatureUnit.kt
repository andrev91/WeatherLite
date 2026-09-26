package com.tehuberz.weather.lite.data.model

enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT,
    ;

    override fun toString(): String =
        when (this) {
            CELSIUS -> "Celsius"
            FAHRENHEIT -> "Fahrenheit"
        }
}
