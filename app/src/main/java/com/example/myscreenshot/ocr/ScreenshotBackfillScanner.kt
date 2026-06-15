package com.example.myscreenshot.ocr

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId

class ScreenshotBackfillScanner(private val context: Context) {
    suspend fun findRecentScreenshots(daysBack: Long = 30): List<SharedInput> = withContext(Dispatchers.IO) {
        val sinceSeconds = LocalDateTime.now()
            .minusDays(daysBack)
            .atZone(ZoneId.systemDefault())
            .toEpochSecond()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val results = mutableListOf<SharedInput>()

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(sinceSeconds.toString()),
            sortOrder,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val bucketIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val displayName = cursor.getString(nameIndex).orEmpty()
                val relativePath = cursor.getString(pathIndex).orEmpty()
                val bucket = cursor.getString(bucketIndex).orEmpty()
                val haystack = "$displayName $relativePath $bucket"
                if (!haystack.contains("screenshot", ignoreCase = true)) continue
                val id = cursor.getLong(idIndex)
                results += SharedInput(
                    uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString()),
                    text = null,
                    mimeType = cursor.getString(mimeIndex) ?: "image/*",
                )
            }
        }
        results
    }
}
