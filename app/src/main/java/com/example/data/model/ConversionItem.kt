package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_history")
data class ConversionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalFileName: String,
    val convertedFileName: String,
    val fileUriString: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String,
    val errorMessage: String? = null,
    val fileSize: Long = 0L
)
