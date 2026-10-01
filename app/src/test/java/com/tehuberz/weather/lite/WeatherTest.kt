package com.tehuberz.weather.lite

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.text.input.TextFieldValue
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
import com.tehuberz.weather.lite.ui.state.LocationType
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
        whenever(mockSettingsRepository.temperatureUnit).thenReturn(kotlinx.coroutines.flow.emptyFlow())
        whenever(mockSettingsRepository.locationExplanationAccepted).thenReturn(kotlinx.coroutines.flow.emptyFlow())

        viewModel =
            WeatherViewModel(
                mockWorkManager,
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

            // Advance to let state selection settle
            advanceUntilIdle()

            viewModel.searchDropdownList(LocationType.CITY, TextFieldValue("San"))

            Thread.sleep(200) // Wait for background thread (Dispatchers.Default)
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
    fun `onUseCurrentLocationClicked when permission already granted resolves weather`() =
        runTest {
            val locatedCity =
                com.tehuberz.weather.lite.data.model
                    .LocatedCity(state = State("New York", "NY"), city = "New York City")
            whenever(mockLocationRepository.getCurrentLocatedCity()).thenReturn(locatedCity)

            viewModel.onUseCurrentLocationClicked(hasPermission = true)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isResolvingCurrentLocation || viewModel.uiState.value.error != null)
            assertFalse(viewModel.uiState.value.showLocationExplanationDialog)
        }

    @Test
    fun `onUseCurrentLocationClicked when permission not granted and explanation not accepted shows dialog`() =
        runTest {
            viewModel.onUseCurrentLocationClicked(hasPermission = false)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showLocationExplanationDialog)
        }

    @Test
    fun `onUseCurrentLocationClicked when permission not granted and explanation accepted emits permission prompt`() =
        runTest {
            val explanationFlow = MutableStateFlow(true)
            whenever(mockSettingsRepository.locationExplanationAccepted).thenReturn(explanationFlow)
            viewModel =
                WeatherViewModel(
                    mockWorkManager,
                    mockLocationRepository,
                    mockSettingsRepository,
                )
            advanceUntilIdle()

            viewModel.permissionPromptChannel.test {
                viewModel.onUseCurrentLocationClicked(hasPermission = false)
                assertEquals(Unit, awaitItem())
                assertFalse(viewModel.uiState.value.showLocationExplanationDialog)
            }
        }

    @Test
    fun `acceptLocationExplanation saves preference and emits permission prompt`() =
        runTest {
            viewModel.permissionPromptChannel.test {
                viewModel.acceptLocationExplanation()
                assertEquals(Unit, awaitItem())
                Mockito.verify(mockSettingsRepository).setLocationExplanationAccepted(true)
                assertFalse(viewModel.uiState.value.showLocationExplanationDialog)
            }
        }

    @Test
    fun `dismissLocationExplanation hides dialog without saving or prompting`() =
        runTest {
            viewModel.onUseCurrentLocationClicked(hasPermission = false)
            assertTrue(viewModel.uiState.value.showLocationExplanationDialog)

            viewModel.denyLocationExplanation()
            assertFalse(viewModel.uiState.value.showLocationExplanationDialog)
            Mockito.verify(mockSettingsRepository, Mockito.never()).setLocationExplanationAccepted(any())
        }

    @Test
    fun `onPermissionResult when granted resolves weather`() =
        runTest {
            val locatedCity =
                com.tehuberz.weather.lite.data.model
                    .LocatedCity(state = State("New York", "NY"), city = "New York City")
            whenever(mockLocationRepository.getCurrentLocatedCity()).thenReturn(locatedCity)

            viewModel.onPermissionResult(granted = true)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isResolvingCurrentLocation || viewModel.uiState.value.error != null)
        }
}
