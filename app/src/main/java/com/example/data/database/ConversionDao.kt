package com.example.data.database

import androidx.room.*
import com.example.data.model.ConversionItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversionDao {
    @Query("SELECT * FROM conversion_history ORDER BY timestamp DESC")
    fun getAllConversions(): Flow<List<ConversionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversion(item: ConversionItem)

    @Delete
    suspend fun deleteConversion(item: ConversionItem)

    @Query("DELETE FROM conversion_history")
    suspend fun clearAll()
}
