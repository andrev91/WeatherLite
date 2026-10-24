package com.tehuberz.weather.lite.util

enum class ApiServiceHost(
    val baseUrl: String,
    val apiKeyConfigName: String,
    val apiKeyPortalUrl: String,
    var isActive: Boolean = false,
) {
    ACCUWEATHER(
        baseUrl = "https://dataservice.accuweather.com/",
        apiKeyConfigName = "ACCUWEATHER_API_KEY",
        apiKeyPortalUrl = "https://developer.accuweather.com/",
    ),
    OPEN_WEATHER_MAP(
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
