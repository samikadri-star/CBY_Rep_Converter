package com.example.data.repository

import com.example.data.database.ConversionDao
import com.example.data.model.ConversionItem
import kotlinx.coroutines.flow.Flow

class ConversionRepository(private val conversionDao: ConversionDao) {
    val allConversions: Flow<List<ConversionItem>> = conversionDao.getAllConversions()

    suspend fun insert(item: ConversionItem) {
        conversionDao.insertConversion(item)
    }

    suspend fun delete(item: ConversionItem) {
        conversionDao.deleteConversion(item)
    }

    suspend fun clearAll() {
        conversionDao.clearAll()
    }
}
