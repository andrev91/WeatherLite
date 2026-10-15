package com.tehuberz.weather.lite.ui.state

import com.tehuberz.weather.lite.data.model.State

data class LocationSelectionState(
    val isLoadingStates: Boolean = false,
    val availableStates: List<State> = emptyList(),
    val selectedState: State? = null,
    val stateSearchQuery: String = "",
    val filteredStates: List<State> = emptyList(),
    val isLoadingCities: Boolean = false,
    val availableCities: List<String> = emptyList(),
    val selectedCity: String? = null,
    val citySearchQuery: String = "",
    val filteredCities: List<String> = emptyList(),
) {
    val displayedStates: List<State>
        get() = filteredStates.ifEmpty { availableStates }

    val displayedCities: List<String>
        get() = filteredCities.ifEmpty { availableCities }

    val showCityDropdown: Boolean
        get() = selectedState != null && availableCities.isNotEmpty()

    val canBookmark: Boolean
        get() = selectedState != null && selectedCity != null
}
