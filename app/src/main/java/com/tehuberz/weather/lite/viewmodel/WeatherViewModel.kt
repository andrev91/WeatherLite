package com.tehuberz.weather.lite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tehuberz.weather.lite.R
import com.tehuberz.weather.lite.data.local.model.Bookmark
import com.tehuberz.weather.lite.data.model.LocatedCity
import com.tehuberz.weather.lite.data.model.TemperatureUnit
import com.tehuberz.weather.lite.data.repository.LocationRepository
import com.tehuberz.weather.lite.data.repository.SettingsRepository
import com.tehuberz.weather.lite.data.repository.WeatherFetchResult
import com.tehuberz.weather.lite.data.repository.WeatherRepository
import com.tehuberz.weather.lite.ui.state.BookmarkState
import com.tehuberz.weather.lite.ui.state.LocationSelectionState
import com.tehuberz.weather.lite.ui.state.LocationType
import com.tehuberz.weather.lite.ui.state.LocationType.CITY
import com.tehuberz.weather.lite.ui.state.LocationType.STATE
import com.tehuberz.weather.lite.ui.state.PermissionRequestState
import com.tehuberz.weather.lite.ui.state.WeatherDataState
import com.tehuberz.weather.lite.ui.state.WeatherUiState
import com.tehuberz.weather.lite.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel
    @Inject
    constructor(
        private val weatherRepository: WeatherRepository,
        private val locationRepository: LocationRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        val uiState: StateFlow<WeatherUiState> field = MutableStateFlow(WeatherUiState())
        private val _bookmarkStateChannel = Channel<BookmarkState>()
        val bookmarkStateChannel = _bookmarkStateChannel.receiveAsFlow()

        private val _permissionPromptChannel = Channel<Unit>(Channel.BUFFERED)
        val permissionPromptChannel = _permissionPromptChannel.receiveAsFlow()

        private var searchJob: Job? = null
        private var weatherJob: Job? = null

        init {
            fetchStateList()
            observeRepositoryStreams()
        }

        private fun observeRepositoryStreams() {
            combine(
                locationRepository.getBookmarks().onStart { emit(emptyList()) },
                settingsRepository.temperatureUnit.onStart { emit(TemperatureUnit.CELSIUS) },
                settingsRepository.locationPermissionRequested.onStart { emit(false) },
            ) { bookmarks, unit, permissionRequested ->
                uiState.update { current ->
                    current.copy(
                        bookmarks = bookmarks,
                        weatherState = current.weatherState.copy(temperatureUnit = unit),
                        locationPermissionRequested = permissionRequested,
                    )
                }
            }.launchIn(viewModelScope)
        }

        fun addBookmark() {
            viewModelScope.launch {
                val state = uiState.value.locationState.selectedState ?: return@launch
                val city = uiState.value.locationState.selectedCity ?: return@launch

                if (locationRepository.isBookmarkDuplicate(state.name, city)) {
                    _bookmarkStateChannel.send(
                        BookmarkState.OnError(UiText.StringResource(R.string.cannot_save_duplicate_bookmark)),
                    )
                    return@launch
                }

                val bookmark =
                    Bookmark(
                        stateName = state.name,
                        stateAbbreviation = state.abbreviation,
                        cityName = city,
                    )
                locationRepository.addBookmark(bookmark)
                _bookmarkStateChannel.send(
                    BookmarkState.OnSuccess(UiText.StringResource(R.string.bookmark_successfully_added)),
                )
            }
        }

        fun removeBookmark(bookmark: Bookmark) {
            viewModelScope.launch {
                locationRepository.removeBookmark(bookmark)
                _bookmarkStateChannel.send(BookmarkState.OnDelete(UiText.StringResource(R.string.bookmark_removed)))
            }
        }

        fun loadBookmark(bookmark: Bookmark) {
            setDropdownSelection(STATE, bookmark.stateName)
            setDropdownSelection(CITY, bookmark.cityName)
        }

        private fun searchStateList(query: String) {
            uiState.update { it.copy(error = null) }
            updateLocationState { currentState ->
                val filteredStates = locationRepository.filterStates(query, currentState.availableStates)
                currentState.copy(stateSearchQuery = query, filteredStates = filteredStates)
            }
        }

        fun clearDropdownSelection(locationType: LocationType) {
            when (locationType) {
                STATE -> {
                    searchJob?.cancel()
                    updateLocationState { currentState ->
                        currentState.copy(
                            selectedState = null,
                            isLoadingStates = false,
                            filteredStates = emptyList(),
                            isLoadingCities = false,
                            selectedCity = null,
                            availableCities = emptyList(),
                            stateSearchQuery = "",
                            citySearchQuery = "",
                        )
                    }
                }

                CITY -> {
                    searchJob?.cancel()
                    updateLocationState { currentState ->
                        currentState.copy(
                            isLoadingCities = false,
                            selectedCity = null,
                            filteredCities = emptyList(),
                            citySearchQuery = "",
                        )
                    }
                }
            }
            updateWeatherState { currentState -> currentState.copy(weatherContent = null) }
            uiState.update { it.copy(error = null) }
        }

        fun setDropdownSelection(
            locationType: LocationType,
            location: String,
        ) {
            when (locationType) {
                STATE -> {
                    searchJob?.cancel()
                    val state = locationRepository.getStateFromString(location) ?: return
                    if (state == uiState.value.locationState.selectedState) return

                    updateLocationState { currentState ->
                        currentState.copy(
                            selectedState = state,
                            isLoadingStates = false,
                            isLoadingCities = true,
                            selectedCity = null,
                            availableCities = emptyList(),
                            stateSearchQuery = state.name,
                            citySearchQuery = "",
                        )
                    }
                    updateWeatherState { currentState -> currentState.copy(weatherContent = null) }
                    uiState.update { it.copy(error = null) }

                    viewModelScope.launch {
                        val cities = locationRepository.getMajorCitiesByState(state.abbreviation)
                        updateLocationState { currentState ->
                            currentState.copy(availableCities = cities, isLoadingCities = false)
                        }
                    }
                }

                CITY -> {
                    searchJob?.cancel()
                    if (location == uiState.value.locationState.selectedCity) return
                    updateLocationState { currentState ->
                        currentState.copy(selectedCity = location, citySearchQuery = location)
                    }
                    updateWeatherState { currentState -> currentState.copy(weatherContent = null) }
                    uiState.update { it.copy(error = null) }
                }
            }
        }

        fun searchDropdownList(
            locationType: LocationType,
            query: String,
        ) {
            when (locationType) {
                STATE -> searchStateList(query)
                CITY -> searchCityList(query)
            }
        }

        private fun searchCityList(query: String) {
            if (uiState.value.locationState.citySearchQuery == query) return

            updateLocationState { currentState -> currentState.copy(citySearchQuery = query) }
            uiState.update { it.copy(error = null) }

            searchJob?.cancel()
            searchJob =
                viewModelScope.launch {
                    val currentState = uiState.value.locationState
                    val filteredCities =
                        locationRepository.searchCities(
                            stateAbbreviation = currentState.selectedState?.abbreviation,
                            query = query,
                            defaultCities = currentState.availableCities,
                        )
                    updateLocationState { state -> state.copy(filteredCities = filteredCities) }
                }
        }

        private fun fetchWeather(
            lat: Double,
            lon: Double,
            keepExistingWeather: Boolean = false,
        ) {
            updateWeatherState { currentState ->
                currentState.copy(
                    weatherContent = if (keepExistingWeather) currentState.weatherContent else null,
                    isLoadingWeather = true,
                )
            }
            uiState.update { it.copy(error = null) }

            weatherJob?.cancel()
            weatherJob =
                weatherRepository
                    .fetchWeather(lat, lon)
                    .onEach { result ->
                        when (result) {
                            is WeatherFetchResult.Loading -> {
                                if (!uiState.value.weatherState.isLoadingWeather) {
                                    updateWeatherState { currentState -> currentState.copy(isLoadingWeather = true) }
                                    uiState.update { it.copy(error = null) }
                                }
                            }

                            is WeatherFetchResult.Success -> {
                                updateWeatherState { currentState ->
                                    currentState.copy(
                                        weatherContent = result.weather,
                                        isLoadingWeather = false,
                                    )
                                }
                                uiState.update { it.copy(error = null) }
                            }

                            is WeatherFetchResult.Error -> {
                                updateWeatherState { currentState -> currentState.copy(isLoadingWeather = false) }
                                uiState.update { it.copy(error = result.error) }
                            }
                        }
                    }.launchIn(viewModelScope)
        }

        fun searchLocation(keepExistingWeather: Boolean = false) {
            val locationState = uiState.value.locationState
            if (!locationState.canBookmark) {
                uiState.update { it.copy(error = UiText.DynamicString("Please select a state and city.")) }
                return
            }
            if (uiState.value.weatherState.isLoadingWeather) return

            updateWeatherState { currentState ->
                currentState.copy(
                    weatherContent = if (keepExistingWeather) currentState.weatherContent else null,
                    isLoadingWeather = true,
                )
            }
            uiState.update { it.copy(error = null) }

            val state = locationState.selectedState?.name ?: return
            val city = locationState.selectedCity ?: ""

            viewModelScope.launch {
                locationRepository
                    .getOrFetchLocation("$city, $state")
                    .collect { result ->
                        result
                            .onSuccess { location ->
                                fetchWeather(location.latitude, location.longitude, keepExistingWeather)
                            }.onFailure { error ->
                                updateWeatherState { currentState -> currentState.copy(isLoadingWeather = false) }
                                uiState.update { it.copy(error = UiText.DynamicString(error.message!!)) }
                            }
                    }
            }
        }

        private fun fetchStateList() {
            if (uiState.value.locationState.isLoadingStates) return

            updateLocationState { currentState ->
                currentState.copy(
                    isLoadingStates = true,
                    selectedState = null,
                    selectedCity = null,
                    availableStates = emptyList(),
                    availableCities = emptyList(),
                )
            }
            uiState.update { it.copy(error = null) }
            val stateData = locationRepository.getStates()
            updateLocationState { currentState ->
                currentState.copy(
                    availableStates = stateData,
                    filteredStates = stateData,
                    isLoadingStates = false,
                )
            }
        }

        private fun updateLocationState(update: (currentState: LocationSelectionState) -> LocationSelectionState) {
            uiState.update { currentState ->
                currentState.copy(locationState = update(currentState.locationState))
            }
        }

        private fun updateWeatherState(update: (currentState: WeatherDataState) -> WeatherDataState) {
            uiState.update { currentState ->
                currentState.copy(weatherState = update(currentState.weatherState))
            }
        }

        fun onUseCurrentLocationClicked(
            hasPermission: Boolean,
            shouldShowRationale: Boolean = false,
        ) {
            if (uiState.value.isResolvingCurrentLocation) return
            uiState.update { it.copy(error = null) }

            if (hasPermission) {
                requestWeatherForCurrentLocation()
                return
            }

            val nextDialogState =
                if (uiState.value.locationPermissionRequested && !shouldShowRationale) {
                    PermissionRequestState.SETTINGS
                } else {
                    PermissionRequestState.EXPLANATION
                }
            uiState.update { it.copy(permissionRequestState = nextDialogState) }
        }

        fun acceptLocationExplanation() {
            uiState.update { it.copy(permissionRequestState = PermissionRequestState.NONE) }
            viewModelScope.launch {
                settingsRepository.setLocationPermissionRequested(true)
                requestLocationPermissionPrompt()
            }
        }

        fun denyLocationExplanation() {
            uiState.update { it.copy(permissionRequestState = PermissionRequestState.NONE) }
        }

        fun dismissLocationSettingsDialog() {
            uiState.update { it.copy(permissionRequestState = PermissionRequestState.NONE) }
        }

        fun onPermissionResult(
            granted: Boolean,
            shouldShowRationale: Boolean = false,
        ) {
            viewModelScope.launch {
                settingsRepository.setLocationPermissionRequested(true)
            }
            if (granted) {
                requestWeatherForCurrentLocation()
            } else {
                uiState.update {
                    it.copy(
                        error = UiText.StringResource(R.string.location_permission_denied_error),
                        permissionRequestState =
                            if (!shouldShowRationale) {
                                PermissionRequestState.SETTINGS
                            } else {
                                PermissionRequestState.NONE
                            },
                    )
                }
            }
        }

        private fun requestLocationPermissionPrompt() {
            viewModelScope.launch {
                _permissionPromptChannel.send(Unit)
            }
        }

        fun requestWeatherForCurrentLocation() {
            if (uiState.value.isResolvingCurrentLocation) return
            uiState.update { it.copy(error = null, isResolvingCurrentLocation = true) }
            viewModelScope.launch {
                try {
                    val locatedCity = locationRepository.getCurrentLocatedCity()
                    if (locatedCity == null) {
                        updateCurrentLocationFailure()
                        return@launch
                    }
                    applyLocatedCityAndFetchWeather(locatedCity)
                } catch (_: Exception) {
                    updateCurrentLocationFailure()
                } finally {
                    uiState.update { it.copy(isResolvingCurrentLocation = false) }
                }
            }
        }

        private fun applyLocatedCityAndFetchWeather(locatedCity: LocatedCity) {
            val currentLocationState = uiState.value.locationState
            val isSameLocation =
                currentLocationState.selectedState == locatedCity.state &&
                    currentLocationState.selectedCity == locatedCity.city

            if (!isSameLocation) {
                val cities = locationRepository.getMajorCitiesByState(locatedCity.state.abbreviation)
                updateLocationState { currentState ->
                    currentState.copy(
                        selectedState = locatedCity.state,
                        isLoadingStates = false,
                        isLoadingCities = false,
                        availableCities = cities,
                        filteredCities = emptyList(),
                        selectedCity = locatedCity.city,
                        stateSearchQuery = locatedCity.state.name,
                        citySearchQuery = locatedCity.city,
                    )
                }
                updateWeatherState { currentState -> currentState.copy(weatherContent = null) }
            }

            uiState.update { it.copy(error = null, isResolvingCurrentLocation = false) }
            val keepExisting = isSameLocation && uiState.value.weatherState.weatherContent != null
            if (locatedCity.latitude != null && locatedCity.longitude != null) {
                fetchWeather(locatedCity.latitude, locatedCity.longitude, keepExistingWeather = keepExisting)
            } else {
                searchLocation(keepExistingWeather = keepExisting)
            }
        }

        private fun updateCurrentLocationFailure() {
            uiState.update {
                it.copy(
                    isResolvingCurrentLocation = false,
                    error =
                        UiText.DynamicString(
                            "Unable to find a location for your current area.",
                        ),
                )
            }
        }

        companion object {
            const val TAG = "MainViewModel"
        }
    }
