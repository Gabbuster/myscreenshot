package com.example.myscreenshot.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "source_documents")
data class SourceDocument(
    @PrimaryKey val id: String,
    val localUri: String?,
    val mimeType: String,
    val extractedText: String,
    val createdAt: Long,
)

