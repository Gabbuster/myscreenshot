package com.example.myscreenshot.ocr

import android.net.Uri

data class SharedInput(
    val uri: Uri?,
    val text: String?,
    val mimeType: String?,
) {
    val sourceType: String
        get() = when {
            text != null -> "text"
            mimeType == "application/pdf" -> "pdf"
            mimeType?.startsWith("image/") == true -> "image"
            else -> "manual"
        }
}

