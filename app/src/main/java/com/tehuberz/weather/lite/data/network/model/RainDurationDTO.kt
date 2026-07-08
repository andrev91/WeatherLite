package com.tehuberz.weather.lite.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RainDurationDTO(
    @SerialName("1h")
    val oneHour: Double? = null
)
