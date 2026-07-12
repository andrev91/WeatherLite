package com.example.adventure.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.tehuberz.weather.lite.data.model.TemperatureUnit
import com.tehuberz.weather.lite.data.repository.SettingsRepository
import com.tehuberz.weather.lite.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var viewModel: SettingsViewModel

    private val temperatureUnitFlow = MutableStateFlow(TemperatureUnit.CELSIUS)

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(settingsRepository.temperatureUnit).thenReturn(temperatureUnitFlow)

        viewModel = SettingsViewModel(settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `temperatureUnit initially reflects repository value`() = runTest {
        viewModel.temperatureUnit.test {
            assertEquals(TemperatureUnit.CELSIUS, awaitItem())

            temperatureUnitFlow.value = TemperatureUnit.FAHRENHEIT
            assertEquals(TemperatureUnit.FAHRENHEIT, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setTemperatureUnit calls repository`() = runTest {
        viewModel.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
        advanceUntilIdle()

        verify(settingsRepository).setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
    }
}
