package com.tehuberz.weather.lite.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tehuberz.weather.lite.data.local.model.Location
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: Location)

    @Query("SELECT * FROM location WHERE name = :searchString LIMIT 1")
    fun getLocationBySearchString(searchString: String) : Flow<Location?>
    @Delete
    fun deleteLocation(location: Location)

}