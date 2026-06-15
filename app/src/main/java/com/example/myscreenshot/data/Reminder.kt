package com.example.myscreenshot.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val sourceType: String,
    val sourceImageUri: String?,
    val ocrText: String,
    val dateTime: Long?,
    val endDateTime: Long? = null,
    val amount: Double?,
    val currency: String?,
    val location: String?,
    val notes: String,
    val confidence: String,
    val calendarSavedAt: Long? = null,
    val alarmSavedAt: Long? = null,
    val tagName: String? = null,
    val tagColor: String? = null,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)
