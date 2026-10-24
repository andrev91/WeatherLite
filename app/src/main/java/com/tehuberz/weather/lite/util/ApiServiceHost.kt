package com.tehuberz.weather.lite.util

enum class ApiServiceHost(
    val providerName: String,
    val baseUrl: String,
    val apiKeyConfigName: String,
    val apiKeyPortalUrl: String,
    var isActive: Boolean = false,
) {
    ACCUWEATHER(
        providerName = "AccuWeather",
        baseUrl = "https://dataservice.accuweather.com/",
        apiKeyConfigName = "ACCUWEATHER_API_KEY",
        apiKeyPortalUrl = "https://developer.accuweather.com/",
    ),
    OPEN_WEATHER_MAP(
        providerName = "OpenWeather",
        baseUrl = "https://api.openweathermap.org/",
        apiKeyConfigName = "OPEN_WEATHER_API_KEY",
        apiKeyPortalUrl = "https://openweathermap.org/api",
        isActive = true,
    ),
    ;

    companion object {
        fun setActive(host: ApiServiceHost) {
            entries.forEach { it.isActive = false }
            host.isActive = true
        }

        fun getActive(): ApiServiceHost = entries.firstOrNull { it.isActive } ?: OPEN_WEATHER_MAP
    }
}
