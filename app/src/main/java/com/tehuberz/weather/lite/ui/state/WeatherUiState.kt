package com.tehuberz.weather.lite.ui.state

import androidx.compose.ui.text.input.TextFieldValue
import com.tehuberz.weather.lite.data.local.model.Bookmark
import com.tehuberz.weather.lite.data.model.State
import com.tehuberz.weather.lite.data.model.TemperatureUnit
import com.tehuberz.weather.lite.util.UiText
import com.tehuberz.weather.lite.viewmodel.WeatherDisplayData

data class WeatherUiState(
    val locationState : LocationSelectionState = LocationSelectionState(),
    val weatherState : WeatherDataState = WeatherDataState(),
    val error: UiText? = null,
    val bookmarks: List<Bookmark> = emptyList(),
    val bookmarkState: BookmarkState? = null,
)

data class LocationSelectionState(
    val isLoadingStates: Boolean = false,
    val availableStates: List<State>? = emptyList(),
    val selectedState: State? = null,
    val stateSearchQuery: TextFieldValue = TextFieldValue(""),
    val filteredStates: List<State> = emptyList(),
    val isLoadingCities: Boolean = false,
    val availableCities: List<String>? = emptyList(),
    val selectedCity: String? = null,
    val citySearchQuery: TextFieldValue = TextFieldValue(""),
    val filteredCities: List<String> = emptyList(),
)

data class WeatherDataState(
    val isLoadingWeather: Boolean = false,
    val displayData: WeatherDisplayData? = null,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
)