package com.example.myscreenshot.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminder_alerts",
    foreignKeys = [
        ForeignKey(
            entity = Reminder::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("reminderId")],
)
data class ReminderAlert(
    @PrimaryKey val id: String,
    val reminderId: String,
    val alertDateTime: Long,
    val alertLabel: String,
    val isScheduled: Boolean,
    val isTriggered: Boolean,
)

