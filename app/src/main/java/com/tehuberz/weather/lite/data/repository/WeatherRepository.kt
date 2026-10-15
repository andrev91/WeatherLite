package com.tehuberz.weather.lite.data.repository

import android.annotation.SuppressLint
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tehuberz.weather.lite.R
import com.tehuberz.weather.lite.data.network.model.OpenWeatherResponseDTO
import com.tehuberz.weather.lite.ui.model.WeatherDataPO
import com.tehuberz.weather.lite.util.UiText
import com.tehuberz.weather.lite.util.UnixTimestampToLocal
import com.tehuberz.weather.lite.worker.WeatherWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

sealed interface WeatherFetchResult {
    data object Loading : WeatherFetchResult

    data class Success(
        val weather: WeatherDataPO,
    ) : WeatherFetchResult

    data class Error(
        val error: UiText,
    ) : WeatherFetchResult
}

@Singleton
class WeatherRepository
    @Inject
    constructor(
        private val workManager: WorkManager,
    ) {
        fun fetchWeather(
            lat: Double,
            lon: Double,
        ): Flow<WeatherFetchResult> {
            val weatherRequest =
                OneTimeWorkRequestBuilder<WeatherWorker>()
                    .setInputData(
                        workDataOf(
                            WeatherWorker.WEATHER_LAT_KEY to lat,
                            WeatherWorker.WEATHER_LON_KEY to lon,
                        ),
                    ).setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).build()

            val requestId = weatherRequest.id
            workManager.enqueueUniqueWork(
                "WeatherLocation_$lat,$lon",
                ExistingWorkPolicy.REPLACE,
                weatherRequest,
            )

            return workManager
                .getWorkInfoByIdFlow(requestId)
                .filterNotNull()
                .filter { it.id == requestId }
                .map { workInfo -> mapWorkInfoToResult(workInfo) }
        }

        private fun mapWorkInfoToResult(workInfo: WorkInfo): WeatherFetchResult =
            when (workInfo.state) {
                WorkInfo.State.SUCCEEDED -> {
                    val outputData = workInfo.outputData
                    val success = outputData.getBoolean(WeatherWorker.OUTPUT_SUCCESS, false)

                    if (success) {
                        val weatherJson = outputData.getString(WeatherWorker.WEATHER_JSON)
                        if (weatherJson != null) {
                            try {
                                val response = Json.decodeFromString<OpenWeatherResponseDTO>(weatherJson)
                                Log.d(TAG, "Successfully parsed weather data from worker.")
                                WeatherFetchResult.Success(mapResponseToWeatherDataPO(response))
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing JSON from worker output", e)
                                WeatherFetchResult.Error(UiText.StringResource(R.string.failed_to_parse_weather_data_error))
                            }
                        } else {
                            Log.e(TAG, "Work succeeded but weather JSON was null.")
                            WeatherFetchResult.Error(UiText.StringResource(R.string.received_empty_success_response_error))
                        }
                    } else {
                        val errorMsg =
                            outputData.getString(WeatherWorker.OUTPUT_ERROR_MESSAGE)
                                ?: "Worker reported failure."
                        Log.e(TAG, "Work succeeded but internal flag was false: $errorMsg")
                        WeatherFetchResult.Error(UiText.DynamicString(errorMsg))
                    }
                }

                WorkInfo.State.FAILED -> {
                    val errorMsg =
                        workInfo.outputData.getString(WeatherWorker.OUTPUT_ERROR_MESSAGE)
                            ?: "Unknown error"
                    Log.e(TAG, "Work failed: $errorMsg")
                    WeatherFetchResult.Error(UiText.DynamicString(errorMsg))
                }

                WorkInfo.State.CANCELLED -> {
                    Log.w(TAG, "Work cancelled.")
                    WeatherFetchResult.Error(UiText.StringResource(R.string.weather_fetch_cancelled_error))
                }

                WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
                    Log.d(TAG, "Work is ${workInfo.state}.")
                    WeatherFetchResult.Loading
                }
            }

        @SuppressLint("DefaultLocale")
        internal fun mapResponseToWeatherDataPO(response: OpenWeatherResponseDTO): WeatherDataPO {
            val formattedTempFahrenheit = "${response.main.temp}°F"
            val formattedTempCelsius = String.format("%.2f°C", (response.main.temp - 32) * 5 / 9)
            val observedTime = UnixTimestampToLocal.execute(response.dt)
            val icon = response.weather.firstOrNull()?.icon
            val weatherIconUrl =
                if (icon.isNullOrBlank()) {
                    null
                } else {
                    "https://openweathermap.org/img/wn/$icon@2x.png"
                }

            return WeatherDataPO(
                weatherDescription = UiText.DynamicString(response.weather.firstOrNull()?.description ?: "No description"),
                weatherIcon = weatherIconUrl,
                temperatureFahrenheit = UiText.DynamicString(formattedTempFahrenheit),
                temperatureCelsius = UiText.DynamicString(formattedTempCelsius),
                observedAt = UiText.DynamicString(observedTime),
            )
        }

        companion object {
            private const val TAG = "WeatherRepository"
        }
    }
