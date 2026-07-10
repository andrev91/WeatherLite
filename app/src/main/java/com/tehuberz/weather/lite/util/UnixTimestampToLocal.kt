package com.tehuberz.weather.lite.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object UnixTimestampToLocal {
    fun execute(timestamp: Long) : String {
        // 1. Convert seconds to milliseconds and create an Instant
        val instant = Instant.ofEpochMilli(timestamp * 1000)

        // 2. Define a formatter (this creates a localized time string like "3:30 PM")
        val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())

        // 3. Format the instant into a readable string
        return formatter.format(instant)
    }
}
