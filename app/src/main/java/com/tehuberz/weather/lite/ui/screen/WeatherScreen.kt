package com.tehuberz.weather.lite.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.tehuberz.weather.lite.R
import com.tehuberz.weather.lite.data.model.State
import com.tehuberz.weather.lite.data.model.TemperatureUnit
import com.tehuberz.weather.lite.ui.model.WeatherDataPO
import com.tehuberz.weather.lite.ui.scaffold.WeatherScaffold
import com.tehuberz.weather.lite.ui.state.LocationSelectionState
import com.tehuberz.weather.lite.ui.state.LocationType
import com.tehuberz.weather.lite.ui.state.WeatherDataState
import com.tehuberz.weather.lite.ui.state.WeatherUiState
import com.tehuberz.weather.lite.ui.theme.AdventureTheme
import com.tehuberz.weather.lite.util.UiText
import com.tehuberz.weather.lite.viewmodel.WeatherViewModel

const val TAG_LOCATION_DROPDOWN = "LocationDropdown"

const val TAG_CITY_DROPDOWN = "CityDropdown"
const val TAG_LOCATION_DROPDOWN_OUTLINE = "LocationDropdownOutline"
const val TAG_WEATHER_DESC = "WeatherDescriptionText"
const val TAG_WEATHER_TEMP = "WeatherTemperatureText"
const val TAG_ERROR_TEXT = "ErrorText"
const val TAG_PROGRESS = "ProgressIndicator"
const val TAG_LOCATION_DESC = "LocationDescriptionText"
const val TAG_REFRESH_BUTTON = "RefreshButton"
const val TAG_CURRENT_LOCATION_BUTTON = "CurrentLocationButton"

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = hiltViewModel(),
    onSettingsClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.bookmarkStateChannel.collect { state ->
            snackbarHostState.showSnackbar(state.message.asString(context), duration = SnackbarDuration.Short)
        }
    }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            viewModel.onPermissionResult(granted)
        }

    LaunchedEffect(viewModel.permissionPromptChannel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.permissionPromptChannel.collect {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        }
    }

    WeatherScaffold(
        uiState = uiState,
        snackBarHostState = snackbarHostState,
        onRemoveBookmark = { viewModel.removeBookmark(it) },
        onLoadBookmark = { viewModel.loadBookmark(it) },
        onSettingsClick = onSettingsClick,
    ) { innerPadding ->
        WeatherScreenContent(
            modifier = Modifier.padding(innerPadding),
            uiState = uiState,
            onDropdownSearch = { locationType, search -> viewModel.searchDropdownList(locationType, search) },
            onDropdownClear = { viewModel.clearDropdownSelection(it) },
            onDropdownSelected = { locationType, location -> viewModel.setDropdownSelection(locationType, location) },
            onRefreshClicked = { viewModel.searchLocation() },
            onAddBookmark = { viewModel.addBookmark() },
            onUseCurrentLocation = {
                viewModel.onUseCurrentLocationClicked(hasLocationPermission(context))
            },
        )
    }

    if (uiState.showLocationExplanationDialog) {
        LocationExplanationDialog(
            onDismiss = { viewModel.denyLocationExplanation() },
            onAllow = { viewModel.acceptLocationExplanation() },
        )
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

@Composable
private fun LocationExplanationDialog(
    onDismiss: () -> Unit,
    onAllow: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.location_permission_dialog_title)) },
        text = { Text(stringResource(R.string.location_permission_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text(stringResource(R.string.location_permission_dialog_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.location_permission_dialog_dismiss))
            }
        },
    )
}

@Composable
fun WeatherScreenContent(
    modifier: Modifier = Modifier,
    uiState: WeatherUiState,
    onDropdownSearch: (LocationType, TextFieldValue) -> Unit,
    onDropdownClear: (LocationType) -> Unit,
    onDropdownSelected: (LocationType, String) -> Unit,
    onRefreshClicked: () -> Unit,
    onAddBookmark: () -> Unit,
    onUseCurrentLocation: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    Column(
        modifier =
            modifier.then(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.weather_screen_open_weather_data_label), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        SearchableDropDown(
            label = stringResource(R.string.weather_screen_us_state_label),
            testTag = TAG_LOCATION_DESC,
            options = uiState.locationState.filteredStates.ifEmpty { uiState.locationState.availableStates!! },
            onClear = { onDropdownClear(LocationType.STATE) },
            searchQuery = uiState.locationState.stateSearchQuery,
            onSearchQueryChanged = { onDropdownSearch(LocationType.STATE, it) },
            isSelected = uiState.locationState.selectedState != null,
            onOptionSelected = { onDropdownSelected(LocationType.STATE, it) },
        ) { it.name }
        Spacer(Modifier.height(8.dp))
        if (uiState.locationState.selectedState != null && !uiState.locationState.availableCities.isNullOrEmpty()) {
            SearchableDropDown(
                label = stringResource(R.string.weather_screen_city_label),
                testTag = TAG_CITY_DROPDOWN,
                options = uiState.locationState.filteredCities.ifEmpty { uiState.locationState.availableCities },
                onClear = { onDropdownClear(LocationType.CITY) },
                searchQuery = uiState.locationState.citySearchQuery,
                onSearchQueryChanged = { onDropdownSearch(LocationType.CITY, it) },
                isSelected = uiState.locationState.selectedCity != null,
                onOptionSelected = { onDropdownSelected(LocationType.CITY, it) },
            ) { it }
            Spacer(Modifier.height(16.dp))
        }
        if ((
                uiState.locationState.isLoadingStates ||
                    uiState.locationState.isLoadingCities ||
                    uiState.weatherState.isLoadingWeather ||
                    uiState.isResolvingCurrentLocation
            ) &&
            uiState.error == null
        ) {
            CircularProgressIndicator(modifier = Modifier.testTag(TAG_PROGRESS))
            Text(text = stringResource(R.string.weather_screen_loading_text), modifier = Modifier.padding(8.dp))
        } else if (uiState.weatherState.weatherContent == null && uiState.error == null) {
            Text(text = stringResource(R.string.weather_screen_weather_location_data_text), modifier = Modifier.padding(8.dp))
        } else if (uiState.error != null) {
            Text(text = uiState.error.asString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag(TAG_ERROR_TEXT))
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (uiState.weatherState.weatherContent != null) {
            WeatherDetails(data = uiState.weatherState.weatherContent, unit = uiState.weatherState.temperatureUnit)
            Spacer(modifier = Modifier.height(8.dp))
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {
            Button(
                onClick = onRefreshClicked,
                modifier = Modifier.testTag(TAG_REFRESH_BUTTON),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            ) {
                Text(
                    text =
                        if (uiState.weatherState.weatherContent !=
                            null
                        ) {
                            stringResource(R.string.weather_screen_refresh_weather_data_label)
                        } else {
                            stringResource(R.string.weather_screen_fetch_weather_data_label)
                        },
                )
            }
            if (uiState.locationState.selectedState != null && uiState.locationState.selectedCity != null) {
                Button(onClick = onAddBookmark, elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)) {
                    Text(stringResource(R.string.bookmark_button_label))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onUseCurrentLocation,
            modifier = Modifier.testTag(TAG_CURRENT_LOCATION_BUTTON),
            enabled = !uiState.isResolvingCurrentLocation && !uiState.weatherState.isLoadingWeather,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        ) {
            Icon(Icons.Filled.Place, contentDescription = stringResource(R.string.cd_use_current_location))
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.weather_screen_use_current_location_label),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SearchableDropDown(
    label: String,
    testTag: String,
    options: List<T>,
    searchQuery: TextFieldValue,
    onSearchQueryChanged: (TextFieldValue) -> Unit,
    onOptionSelected: (String) -> Unit,
    onClear: () -> Unit,
    isSelected: Boolean = false,
    optionToString: (T) -> String = { it.toString() },
) {
    var expanded by remember { mutableStateOf(false) }
    val focusController = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(modifier = Modifier.fillMaxWidth(0.8f)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = it
            },
        ) {
            OutlinedTextField(
                modifier =
                    Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth()
                        .testTag(testTag),
                value = searchQuery,
                onValueChange = {
                    onSearchQueryChanged(it)
                    expanded = true // Keep the dropdown open while searching
                },
                label = { Text(label) },
                trailingIcon = {
                    if (isSelected) {
                        IconButton(onClick = {
                            onClear()
                            expanded = false
                        }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.cd_clear_selection))
                        }
                    } else {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                singleLine = true,
            )
            if (options.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(optionToString(option)) },
                            onClick = {
                                keyboardController?.hide()
                                focusController.clearFocus()
                                expanded = false
                                onOptionSelected(optionToString(option))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherDetails(
    data: WeatherDataPO,
    unit: TemperatureUnit = TemperatureUnit.CELSIUS,
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = data.weatherDescription.asString(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.testTag(TAG_WEATHER_DESC),
            )
            if (!data.weatherIcon.isNullOrBlank()) {
                AsyncImage(
                    model = data.weatherIcon,
                    contentDescription = stringResource(R.string.cd_weather_icon),
                    modifier = Modifier.size(48.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text =
                    stringResource(R.string.weather_screen_temperature_label) +
                        if (unit == TemperatureUnit.CELSIUS) data.temperatureCelsius.asString() else data.temperatureFahrenheit.asString(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(TAG_WEATHER_TEMP),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.weather_screen_observed_at_label, data.observedAt.asString()),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWeatherScreenContent_Loading() {
    AdventureTheme {
        WeatherScreenContent(
            uiState =
                WeatherUiState(
                    LocationSelectionState(isLoadingCities = true, isLoadingStates = true),
                    WeatherDataState(isLoadingWeather = true),
                ),
            onDropdownSelected = { _, _ -> },
            onDropdownSearch = { _, _ -> },
            onRefreshClicked = {},
            onDropdownClear = {},
            onAddBookmark = {},
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun PreviewWeatherScreenContent_Loading_Landscape() {
    AdventureTheme {
        WeatherScreenContent(
            uiState =
                WeatherUiState(
                    LocationSelectionState(isLoadingCities = true, isLoadingStates = true),
                    WeatherDataState(isLoadingWeather = true),
                ),
            onDropdownSelected = { _, _ -> },
            onDropdownSearch = { _, _ -> },
            onRefreshClicked = {},
            onDropdownClear = {},
            onAddBookmark = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWeatherScreenContent_Success() {
    AdventureTheme(darkTheme = true) {
        Surface {
            WeatherScreenContent(
                uiState =
                    WeatherUiState(
                        weatherState =
                            WeatherDataState(
                                isLoadingWeather = false,
                                weatherContent =
                                    WeatherDataPO(
                                        temperatureFahrenheit = UiText.DynamicString("77°F"),
                                        temperatureCelsius = UiText.DynamicString("25°C"),
                                        weatherDescription = UiText.DynamicString("Sunny"),
                                        weatherIcon = "https://openweathermap.org/img/wn/01d@2x.png",
                                        observedAt = UiText.DynamicString("14:30"),
                                    ),
                            ),
                        locationState =
                            LocationSelectionState(
                                selectedState = State("Georgia", "GA"),
                                selectedCity = "Dunwoody",
                                availableCities = listOf("Dunwoody", "Powder Springs, Marietta"),
                            ),
                    ),
                onDropdownSelected = { _, _ -> },
                onDropdownSearch = { _, _ -> },
                onRefreshClicked = {},
                onDropdownClear = {},
                onAddBookmark = {},
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun PreviewWeatherScreenContent_Success_Landscape() {
    AdventureTheme(darkTheme = true) {
        Surface {
            WeatherScreenContent(
                uiState =
                    WeatherUiState(
                        weatherState =
                            WeatherDataState(
                                isLoadingWeather = false,
                                weatherContent =
                                    WeatherDataPO(
                                        temperatureFahrenheit = UiText.DynamicString("77°F"),
                                        temperatureCelsius = UiText.DynamicString("25°C"),
                                        weatherDescription = UiText.DynamicString("Sunny"),
                                        weatherIcon = "https://openweathermap.org/img/wn/01d@2x.png",
                                        observedAt = UiText.DynamicString("14:30"),
                                    ),
                            ),
                        locationState =
                            LocationSelectionState(
                                selectedState = State("Georgia", "GA"),
                                selectedCity = "Dunwoody",
                                availableCities = listOf("Dunwoody", "Powder Springs, Marietta"),
                            ),
                    ),
                onDropdownSelected = { _, _ -> },
                onDropdownSearch = { _, _ -> },
                onRefreshClicked = {},
                onDropdownClear = {},
                onAddBookmark = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWeatherScreenContent_Error() {
    AdventureTheme {
        WeatherScreenContent(
            uiState =
                WeatherUiState(
                    weatherState = WeatherDataState(isLoadingWeather = false),
                    locationState = LocationSelectionState(isLoadingStates = false),
                    error = UiText.DynamicString("Network Error"),
                ),
            onDropdownSelected = { _, _ -> },
            onDropdownSearch = { _, _ -> },
            onRefreshClicked = {},
            onDropdownClear = {},
            onAddBookmark = {},
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun PreviewWeatherScreenContent_Error_Landscape() {
    AdventureTheme {
        WeatherScreenContent(
            uiState =
                WeatherUiState(
                    weatherState = WeatherDataState(isLoadingWeather = false),
                    locationState = LocationSelectionState(isLoadingStates = false),
                    error = UiText.DynamicString("Network Error"),
                ),
            onDropdownSelected = { _, _ -> },
            onDropdownSearch = { _, _ -> },
            onRefreshClicked = {},
            onDropdownClear = {},
            onAddBookmark = {},
        )
    }
}
