package com.tehuberz.weather.lite.ui.state

import com.tehuberz.weather.lite.data.local.model.Bookmark
import com.tehuberz.weather.lite.util.UiText

data class WeatherUiState(
    val locationState: LocationSelectionState = LocationSelectionState(),
    val weatherState: WeatherDataState = WeatherDataState(),
    val error: UiText? = null,
    val bookmarks: List<Bookmark> = emptyList(),
    val isResolvingCurrentLocation: Boolean = false,
    val permissionRequestState: PermissionRequestState = PermissionRequestState.NONE,
    val locationPermissionRequested: Boolean = false,
) {
    val isLoading: Boolean
        get() =
            (
                locationState.isLoadingStates ||
                    locationState.isLoadingCities ||
                    weatherState.isLoadingWeather ||
                    isResolvingCurrentLocation
            ) && error == null

    val canUseCurrentLocation: Boolean
        get() = !isResolvingCurrentLocation && !weatherState.isLoadingWeather
}
