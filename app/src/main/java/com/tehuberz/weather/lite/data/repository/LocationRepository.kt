package com.tehuberz.weather.lite.data.repository

import android.content.Context
import android.util.Log
import com.tehuberz.weather.lite.data.local.BookmarkDao
import com.tehuberz.weather.lite.data.local.LocationDao
import com.tehuberz.weather.lite.data.local.model.Bookmark
import com.tehuberz.weather.lite.data.local.model.Location
import com.tehuberz.weather.lite.data.location.LocationProvider
import com.tehuberz.weather.lite.data.model.LocatedCity
import com.tehuberz.weather.lite.data.model.State
import com.tehuberz.weather.lite.data.model.StateCities
import com.tehuberz.weather.lite.data.network.LocationRemoteDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import javax.inject.Inject

class LocationRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val locationDao: LocationDao,
        private val bookmarkDao: BookmarkDao,
        private val locationRemoteDataSource: LocationRemoteDataSource,
        private val locationProvider: LocationProvider,
    ) {
        fun getBookmarks(): Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()

        suspend fun addBookmark(bookmark: Bookmark) {
            bookmarkDao.insert(bookmark)
        }

        suspend fun removeBookmark(bookmark: Bookmark) {
            bookmarkDao.delete(bookmark)
        }

        suspend fun isBookmarkDuplicate(
            state: String,
            city: String,
        ): Boolean = bookmarkDao.getBookmarkByStateAndCity(state, city) != null

        fun getOrFetchLocation(name: String): Flow<Result<Location>> =
            flow {
                val cachedLocation = locationDao.getLocationBySearchString(name).firstOrNull()
                if (cachedLocation != null) {
                    emit(Result.success(cachedLocation))
                    return@flow
                }

                locationRemoteDataSource.fetchLocationCoordinates(name).collect { result ->
                    result
                        .onSuccess { coordinates ->
                            val location = Location(name = name, latitude = coordinates.first, longitude = coordinates.second)
                            locationDao.insertLocation(location)
                            emit(Result.success(location))
                        }.onFailure { error ->
                            emit(Result.failure(error))
                        }
                }
            }

        private val citiesData: Map<String, StateCities> by lazy {
            try {
                val json =
                    context.assets
                        .open("us_state_cities.json")
                        .bufferedReader()
                        .use { it.readText() }
                val parsedData: Map<String, StateCities> = Json.Default.decodeFromString(json)
                return@lazy parsedData
            } catch (e: Exception) {
                Log.e("LocationRepository", "Error loading cities data", e)
                return@lazy emptyMap()
            }
        }

        private val statesData: List<State> by lazy {
            try {
                val json =
                    context.assets
                        .open("us_state.json")
                        .bufferedReader()
                        .use { it.readText() }
                val parsedData: List<State> = Json.Default.decodeFromString(json)
                return@lazy parsedData
            } catch (e: Exception) {
                Log.e("LocationRepository", "Error loading states data", e)
                return@lazy emptyList()
            }
        }

        fun getStateFromString(s: String): State? = statesData.find { it.name == s }

        fun getStates(): List<State> = statesData

        fun getCities(): Map<String, StateCities> = citiesData

        /**
         * Returns a list of ALL cities in the given state.
         * Expected param for state is the abbreviation. IE "CA" for California
         */
        fun getCitiesByState(state: String): List<String> = citiesData[state]?.allCities ?: emptyList()

        /**
         * Returns a list of MAJOR cities in the given state.
         * Expected param for state is the abbreviation. IE "CA" for California
         */
        fun getMajorCitiesByState(state: String): List<String> = citiesData[state]?.majorCities ?: emptyList()

        /**
         * Resolves a GPS coordinate to the nearest place that exists in the app's
         * internal US state/city lists, so the dropdowns stay consistent.
         */
        suspend fun findNearestCity(
            lat: Double,
            lon: Double,
        ): LocatedCity? {
            val results =
                try {
                    locationRemoteDataSource.reverseGeocode(lat, lon)
                } catch (e: Exception) {
                    Log.e("LocationRepository", "Reverse geocoding failed", e)
                    return null
                }
            if (results.isEmpty()) return null

            val best = results[0]
            val state =
                best.state?.let { stateQuery ->
                    statesData.find {
                        it.name.equals(stateQuery, ignoreCase = true) ||
                            it.abbreviation.equals(stateQuery, ignoreCase = true)
                    }
                }
            if (state == null || !best.country.equals("us", ignoreCase = true)) return null

            val cityName =
                citiesData[state.abbreviation]
                    ?.allCities
                    ?.firstOrNull { it.equals(best.name, ignoreCase = true) }
                    ?: citiesData[state.abbreviation]?.majorCities?.firstOrNull()
                    ?: best.name

            return LocatedCity(state = state, city = cityName)
        }

        /**
         * Obtains device GPS coordinates via [LocationProvider] and resolves them
         * to the nearest supported US city in the app's internal database.
         */
        suspend fun getCurrentLocatedCity(): LocatedCity? {
            val coordinates = locationProvider.getLastKnownCoordinates() ?: return null
            return findNearestCity(coordinates.latitude, coordinates.longitude)
        }
    }
