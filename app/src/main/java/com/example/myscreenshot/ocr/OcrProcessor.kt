package com.example.myscreenshot.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class OcrProcessor(private val context: Context) {
    private val pdfRenderer = PdfToBitmapRenderer(context)

    suspend fun process(input: SharedInput): OcrResult = withContext(Dispatchers.IO) {
        input.text?.let {
            return@withContext OcrResult(it, input.sourceType, "Plain text shared")
        }

        val uri = input.uri ?: return@withContext OcrResult("", "manual", "No input found")
        val bitmap = if (input.mimeType == "application/pdf") {
            pdfRenderer.renderFirstPage(uri)
        } else {
            loadBitmap(uri)
        } ?: return@withContext OcrResult("", input.sourceType, "Could not read shared file")

        val text = runTextRecognition(bitmap)
        val note = if (input.mimeType == "application/pdf") "First page processed" else "Image processed locally"
        OcrResult(text, input.sourceType, note)
    }

    private fun loadBitmap(uri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

    private suspend fun runTextRecognition(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return suspendTask(recognizer.process(image)).text
    }

    private suspend fun <T> suspendTask(task: Task<T>): T =
        suspendCancellableCoroutine { continuation ->
            task.addOnSuccessListener { continuation.resume(it) }
            task.addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
        }
}

data class OcrResult(
    val text: String,
    val sourceType: String,
    val note: String,
)

