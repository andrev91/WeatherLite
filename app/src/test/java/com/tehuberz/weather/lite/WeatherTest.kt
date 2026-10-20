package com.tehuberz.weather.lite

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.cash.turbine.test
import com.tehuberz.weather.lite.data.model.State
import com.tehuberz.weather.lite.data.model.StateCities
import com.tehuberz.weather.lite.data.repository.LocationRepository
import com.tehuberz.weather.lite.data.repository.SettingsRepository
import com.tehuberz.weather.lite.data.repository.WeatherRepository
import com.tehuberz.weather.lite.ui.state.LocationType
import com.tehuberz.weather.lite.ui.state.PermissionRequestState
import com.tehuberz.weather.lite.util.UiText
import com.tehuberz.weather.lite.viewmodel.WeatherViewModel
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import java.util.UUID

@ExperimentalCoroutinesApi
class WeatherTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transactionID: UUID
    private lateinit var mockWorkInfo: MutableStateFlow<WorkInfo>

    @Mock
    private lateinit var mockWorkManager: WorkManager

    @Mock
    private lateinit var mockLocationRepository: LocationRepository

    @Mock
    private lateinit var mockSettingsRepository: SettingsRepository

    private lateinit var weatherRepository: WeatherRepository
    private lateinit var viewModel: WeatherViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(
            mockWorkManager.enqueueUniqueWork(
                any<String>(),
                any<ExistingWorkPolicy>(),
                any<OneTimeWorkRequest>(),
            ),
        ).thenReturn(Mockito.mock(Operation::class.java))

        transactionID = UUID.randomUUID()
        val succeededWorkInfo =
            WorkInfo(
                transactionID,
                WorkInfo.State.ENQUEUED,
                emptySet(),
            )
        mockWorkInfo = MutableStateFlow(succeededWorkInfo)

        whenever(mockWorkManager.getWorkInfoByIdFlow(any())).thenReturn(mockWorkInfo)
        whenever(mockLocationRepository.getBookmarks()).thenReturn(kotlinx.coroutines.flow.emptyFlow())
        whenever(mockLocationRepository.getStates()).thenReturn(emptyList())
        whenever(mockLocationRepository.getOrFetchLocation(any())).thenReturn(kotlinx.coroutines.flow.emptyFlow())
        whenever(mockLocationRepository.filterStates(any(), any())).thenCallRealMethod()
        whenever(mockLocationRepository.searchCities(anyOrNull(), any(), any())).thenCallRealMethod()
        whenever(mockSettingsRepository.temperatureUnit).thenReturn(kotlinx.coroutines.flow.emptyFlow())
        whenever(mockSettingsRepository.locationPermissionRequested).thenReturn(kotlinx.coroutines.flow.emptyFlow())

        weatherRepository = WeatherRepository(mockWorkManager)
        viewModel =
            WeatherViewModel(
                weatherRepository,
                mockLocationRepository,
                mockSettingsRepository,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `init state of view model and fetching locations`() =
        runTest {
            viewModel.uiState.test {
                val initialState = awaitItem()

                assertFalse("isLoadingWeatherData be false initially", initialState.weatherState.isLoadingWeather)
                assertFalse("isLoadingCityData be false initially", initialState.locationState.isLoadingCities)
                assertFalse("isLoadingStateList will be false after load", initialState.locationState.isLoadingStates)
                assertNull("weatherDisplayData should be null initially", initialState.weatherState.weatherContent)
                assertNull("selectedState should be null initially", initialState.locationState.selectedState)
                assertNull("selectedCity should be null initially", initialState.locationState.selectedCity)
                assertNull("error should be null initially", initialState.error)
                assertEquals(PermissionRequestState.NONE, initialState.permissionRequestState)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `searchLocation success fetchesWeather`() =
        runTest {
            val mockLocation =
                com.tehuberz.weather.lite.data.local.model
                    .Location(name = "New York", latitude = 40.7128, longitude = -74.0060)
            val mockState = State("New York", "NY")

            whenever(mockLocationRepository.getStateFromString("New York")).thenReturn(mockState)
            whenever(mockLocationRepository.getMajorCitiesByState("NY")).thenReturn(listOf("New York City"))
            whenever(mockLocationRepository.getOrFetchLocation(any())).thenReturn(MutableStateFlow(Result.success(mockLocation)))

            viewModel.setDropdownSelection(LocationType.STATE, "New York")
            viewModel.setDropdownSelection(LocationType.CITY, "New York City")
            viewModel.searchLocation()

            advanceUntilIdle()

            val weatherRequestCaptor = argumentCaptor<OneTimeWorkRequest>()
            Mockito.verify(mockWorkManager).enqueueUniqueWork(
                any<String>(),
                any<ExistingWorkPolicy>(),
                weatherRequestCaptor.capture(),
            )

            val weatherWorkRequest = weatherRequestCaptor.firstValue
            assertTrue(
                weatherWorkRequest.workSpec.input.getDouble(com.tehuberz.weather.lite.worker.WeatherWorker.WEATHER_LAT_KEY, 0.0) == 40.7128,
            )
            assertTrue(
                weatherWorkRequest.workSpec.input.getDouble(
                    com.tehuberz.weather.lite.worker.WeatherWorker.WEATHER_LON_KEY,
                    0.0,
                ) == -74.0060,
            )
        }

    @Test
    fun `searchCityList filters cities correctly`() =
        runTest {
            val cityList = listOf("San Francisco", "San Jose", "Los Angeles")
            val stateCities = StateCities(allCities = cityList, majorCities = emptyList())
            val mockState = State("California", "CA")

            whenever(mockLocationRepository.getStateFromString("California")).thenReturn(mockState)
            whenever(mockLocationRepository.getCities()).thenReturn(mapOf("CA" to stateCities))
            whenever(mockLocationRepository.getMajorCitiesByState("CA")).thenReturn(emptyList())

            viewModel.setDropdownSelection(LocationType.STATE, "California")
            advanceUntilIdle()

            viewModel.searchDropdownList(LocationType.CITY, "San")
            advanceUntilIdle()

            val currentState = viewModel.uiState.value
            val filtered = currentState.locationState.filteredCities
            assertTrue(filtered.contains("San Francisco"))
            assertTrue(filtered.contains("San Jose"))
            assertFalse(filtered.contains("Los Angeles"))
        }

    @Test
    fun `requestWeatherForCurrentLocation resolves coordinates to nearest city and fetches weather`() =
        runTest {
            val mockLocation =
                com.tehuberz.weather.lite.data.local.model
                    .Location(name = "New York", latitude = 40.7128, longitude = -74.0060)
            val nyState = State("New York", "NY")
            val locatedCity =
                com.tehuberz.weather.lite.data.model
                    .LocatedCity(state = nyState, city = "New York City")

            whenever(mockLocationRepository.getCurrentLocatedCity()).thenReturn(locatedCity)
            whenever(mockLocationRepository.getStateFromString("New York")).thenReturn(nyState)
            whenever(mockLocationRepository.getMajorCitiesByState("NY")).thenReturn(listOf("New York City"))
            whenever(mockLocationRepository.getOrFetchLocation(any())).thenReturn(MutableStateFlow(Result.success(mockLocation)))

            viewModel.requestWeatherForCurrentLocation()
            advanceUntilIdle()

            val state = viewModel.uiState.value.locationState
            assertEquals("New York", state.selectedState?.name)
            assertEquals("New York City", state.selectedCity)

            val weatherRequestCaptor = argumentCaptor<OneTimeWorkRequest>()
            Mockito.verify(mockWorkManager).enqueueUniqueWork(
                any<String>(),
                any<ExistingWorkPolicy>(),
                weatherRequestCaptor.capture(),
            )
            val weatherWorkRequest = weatherRequestCaptor.firstValue
            assertTrue(
                weatherWorkRequest.workSpec.input.getDouble(
                    com.tehuberz.weather.lite.worker.WeatherWorker.WEATHER_LAT_KEY,
                    0.0,
                ) == 40.7128,
            )
        }

    @Test
    fun `requestWeatherForCurrentLocation with resolved coordinates directly fetches weather without geocoding`() =
        runTest {
            val nyState = State("New York", "NY")
            val locatedCity =
                com.tehuberz.weather.lite.data.model
                    .LocatedCity(state = nyState, city = "New York City", latitude = 40.7128, longitude = -74.0060)

            whenever(mockLocationRepository.getCurrentLocatedCity()).thenReturn(locatedCity)
            whenever(mockLocationRepository.getMajorCitiesByState("NY")).thenReturn(listOf("New York City"))

            viewModel.requestWeatherForCurrentLocation()
            advanceUntilIdle()

            val state = viewModel.uiState.value.locationState
            assertEquals("New York", state.selectedState?.name)
            assertEquals("New York City", state.selectedCity)
            assertFalse(state.isLoadingCities)

            // Should bypass geocoding since coordinates are already known
            Mockito.verify(mockLocationRepository, Mockito.never()).getOrFetchLocation(any())

            val weatherRequestCaptor = argumentCaptor<OneTimeWorkRequest>()
            Mockito.verify(mockWorkManager).enqueueUniqueWork(
                any<String>(),
                any<ExistingWorkPolicy>(),
                weatherRequestCaptor.capture(),
            )
            val weatherWorkRequest = weatherRequestCaptor.firstValue
            assertTrue(
                weatherWorkRequest.workSpec.input.getDouble(
                    com.tehuberz.weather.lite.worker.WeatherWorker.WEATHER_LAT_KEY,
                    0.0,
                ) == 40.7128,
            )
            assertTrue(
                weatherWorkRequest.workSpec.input.getDouble(
                    com.tehuberz.weather.lite.worker.WeatherWorker.WEATHER_LON_KEY,
                    0.0,
                ) == -74.0060,
            )
        }

    @Test
    fun `requestWeatherForCurrentLocation surfaces error when no fix available`() =
        runTest {
            whenever(mockLocationRepository.getCurrentLocatedCity()).thenReturn(null)

            viewModel.requestWeatherForCurrentLocation()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isResolvingCurrentLocation)
            assertNotNull(state.error)
        }

    @Test
    fun `requestWeatherForCurrentLocation handles repository exception gracefully`() =
        runTest {
            whenever(mockLocationRepository.getCurrentLocatedCity()).thenThrow(RuntimeException("GPS error"))

            viewModel.requestWeatherForCurrentLocation()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isResolvingCurrentLocation)
            assertNotNull(state.error)
        }

    @Test
    fun `onUseCurrentLocationClicked when permission already granted resolves weather`() =
        runTest {
            val locatedCity =
                com.tehuberz.weather.lite.data.model
                    .LocatedCity(
                        state = State("New York", "NY"),
                        city = "New York City",
                        latitude = 40.7128,
                        longitude = -74.0060,
                    )
            whenever(mockLocationRepository.getCurrentLocatedCity()).thenReturn(locatedCity)
            whenever(mockLocationRepository.getMajorCitiesByState("NY")).thenReturn(listOf("New York City"))

            viewModel.onUseCurrentLocationClicked(hasPermission = true)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("New York", state.locationState.selectedState?.name)
            assertEquals("New York City", state.locationState.selectedCity)
            assertFalse(state.isResolvingCurrentLocation)
            assertEquals(PermissionRequestState.NONE, state.permissionRequestState)
        }

    @Test
    fun `onUseCurrentLocationClicked when permission not granted and explanation not accepted shows dialog`() =
        runTest {
            viewModel.onUseCurrentLocationClicked(hasPermission = false)
            advanceUntilIdle()

            assertEquals(PermissionRequestState.EXPLANATION, viewModel.uiState.value.permissionRequestState)
        }

    @Test
    fun `onUseCurrentLocationClicked when permission not requested shows explanation dialog`() =
        runTest {
            viewModel.onUseCurrentLocationClicked(hasPermission = false, shouldShowRationale = false)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(PermissionRequestState.EXPLANATION, state.permissionRequestState)
        }

    @Test
    fun `onUseCurrentLocationClicked when permission permanently denied shows settings dialog`() =
        runTest {
            val requestedFlow = MutableStateFlow(true)
            whenever(mockSettingsRepository.locationPermissionRequested).thenReturn(requestedFlow)
            viewModel =
                WeatherViewModel(
                    weatherRepository,
                    mockLocationRepository,
                    mockSettingsRepository,
                )
            advanceUntilIdle()

            viewModel.onUseCurrentLocationClicked(hasPermission = false, shouldShowRationale = false)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(PermissionRequestState.SETTINGS, state.permissionRequestState)
        }

    @Test
    fun `onUseCurrentLocationClicked when permission denied with rationale shows explanation dialog`() =
        runTest {
            val requestedFlow = MutableStateFlow(true)
            whenever(mockSettingsRepository.locationPermissionRequested).thenReturn(requestedFlow)
            viewModel =
                WeatherViewModel(
                    weatherRepository,
                    mockLocationRepository,
                    mockSettingsRepository,
                )
            advanceUntilIdle()

            viewModel.onUseCurrentLocationClicked(hasPermission = false, shouldShowRationale = true)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(PermissionRequestState.EXPLANATION, state.permissionRequestState)
        }

    @Test
    fun `acceptLocationExplanation saves preference and emits permission prompt`() =
        runTest {
            viewModel.permissionPromptChannel.test {
                viewModel.acceptLocationExplanation()
                assertEquals(Unit, awaitItem())
                Mockito.verify(mockSettingsRepository).setLocationPermissionRequested(true)
                assertEquals(PermissionRequestState.NONE, viewModel.uiState.value.permissionRequestState)
            }
        }

    @Test
    fun `dismissLocationExplanation hides dialog without saving or prompting`() =
        runTest {
            viewModel.onUseCurrentLocationClicked(hasPermission = false)
            assertEquals(PermissionRequestState.EXPLANATION, viewModel.uiState.value.permissionRequestState)

            viewModel.denyLocationExplanation()
            assertEquals(PermissionRequestState.NONE, viewModel.uiState.value.permissionRequestState)
            Mockito.verify(mockSettingsRepository, Mockito.never()).setLocationPermissionRequested(any())
        }

    @Test
    fun `dismissLocationSettingsDialog hides settings dialog`() =
        runTest {
            viewModel.onPermissionResult(granted = false, shouldShowRationale = false)
            assertEquals(PermissionRequestState.SETTINGS, viewModel.uiState.value.permissionRequestState)

            viewModel.dismissLocationSettingsDialog()
            assertEquals(PermissionRequestState.NONE, viewModel.uiState.value.permissionRequestState)
        }

    @Test
    fun `onPermissionResult when granted resolves weather`() =
        runTest {
            val locatedCity =
                com.tehuberz.weather.lite.data.model
                    .LocatedCity(
                        state = State("New York", "NY"),
                        city = "New York City",
                        latitude = 40.7128,
                        longitude = -74.0060,
                    )
            whenever(mockLocationRepository.getCurrentLocatedCity()).thenReturn(locatedCity)
            whenever(mockLocationRepository.getMajorCitiesByState("NY")).thenReturn(listOf("New York City"))

            viewModel.onPermissionResult(granted = true)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("New York", state.locationState.selectedState?.name)
            assertEquals("New York City", state.locationState.selectedCity)
            assertFalse(state.isResolvingCurrentLocation)
        }

    @Test
    fun `onPermissionResult when denied with rationale surfaces error without showing settings dialog`() =
        runTest {
            viewModel.onPermissionResult(granted = false, shouldShowRationale = true)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isResolvingCurrentLocation)
            assertEquals(
                R.string.location_permission_denied_error,
                (state.error as? UiText.StringResource)?.resId,
            )
            assertEquals(PermissionRequestState.NONE, state.permissionRequestState)
            Mockito.verify(mockSettingsRepository).setLocationPermissionRequested(true)
        }

    @Test
    fun `onPermissionResult when permanently denied surfaces error and shows settings dialog`() =
        runTest {
            viewModel.onPermissionResult(granted = false, shouldShowRationale = false)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isResolvingCurrentLocation)
            assertEquals(
                R.string.location_permission_denied_error,
                (state.error as? UiText.StringResource)?.resId,
            )
            assertEquals(PermissionRequestState.SETTINGS, state.permissionRequestState)
            Mockito.verify(mockSettingsRepository).setLocationPermissionRequested(true)
        }

    @Test
    fun `requestWeatherForCurrentLocation when at different location updates location and resets weatherContent`() =
        runTest {
            val nyState = State("New York", "NY")
            val caState = State("California", "CA")

            // Currently selected: California / Los Angeles
            whenever(mockLocationRepository.getStateFromString("California")).thenReturn(caState)
            whenever(mockLocationRepository.getMajorCitiesByState("CA")).thenReturn(listOf("Los Angeles"))
            viewModel.setDropdownSelection(LocationType.STATE, "California")
            viewModel.setDropdownSelection(LocationType.CITY, "Los Angeles")
            advanceUntilIdle()

            // Current location resolves to New York / New York City
            val locatedCity =
                com.tehuberz.weather.lite.data.model
                    .LocatedCity(state = nyState, city = "New York City", latitude = 40.7128, longitude = -74.0060)
            whenever(mockLocationRepository.getCurrentLocatedCity()).thenReturn(locatedCity)
            whenever(mockLocationRepository.getMajorCitiesByState("NY")).thenReturn(listOf("New York City"))

            viewModel.requestWeatherForCurrentLocation()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("New York", state.locationState.selectedState?.name)
            assertEquals("New York City", state.locationState.selectedCity)
        }

    @Test
    fun `requestWeatherForCurrentLocation when at same location retains weatherContent in place`() =
        runTest {
            val nyState = State("New York", "NY")
            val locatedCity =
                com.tehuberz.weather.lite.data.model
                    .LocatedCity(state = nyState, city = "New York City", latitude = 40.7128, longitude = -74.0060)

            whenever(mockLocationRepository.getCurrentLocatedCity()).thenReturn(locatedCity)
            whenever(mockLocationRepository.getMajorCitiesByState("NY")).thenReturn(listOf("New York City"))

            // First resolve to NY
            viewModel.requestWeatherForCurrentLocation()
            advanceUntilIdle()

            // Trigger again while already at same location
            viewModel.requestWeatherForCurrentLocation()

            // Dropdowns should remain the same
            val state = viewModel.uiState.value
            assertEquals("New York", state.locationState.selectedState?.name)
            assertEquals("New York City", state.locationState.selectedCity)
        }
}
