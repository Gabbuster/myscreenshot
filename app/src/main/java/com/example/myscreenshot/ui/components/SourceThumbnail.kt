package com.example.myscreenshot.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SourceThumbnail(
    sourceType: String,
    modifier: Modifier = Modifier.size(58.dp),
    sourceUri: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    thumbnailSize: Int = 180,
    showSheen: Boolean = true,
) {
    val context = LocalContext.current
    val bitmap = produceState<Bitmap?>(initialValue = null, sourceUri) {
        value = sourceUri
            ?.takeIf { sourceType.equals("image", ignoreCase = true) || it.startsWith("content:") || it.startsWith("file:") }
            ?.let { uriString ->
                runCatching {
                    val uri = Uri.parse(uriString)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.contentResolver.loadThumbnail(uri, android.util.Size(thumbnailSize, thumbnailSize), null)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                }.getOrElse {
                    runCatching {
                        context.contentResolver.openInputStream(Uri.parse(uriString))?.use(BitmapFactory::decodeStream)
                    }.getOrNull()
                }
            }
    }.value

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.26f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Source screenshot",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
            if (showSheen) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.12f),
                        topLeft = Offset.Zero,
                        size = size,
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.18f),
                        topLeft = Offset(size.width * 0.08f, size.height * 0.08f),
                        size = Size(size.width * 0.28f, 2.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    )
                }
            }
        } else {
            PlaceholderThumbnail(sourceType = sourceType)
        }
    }
}

@Composable
private fun PlaceholderThumbnail(sourceType: String) {
    val paper = MaterialTheme.colorScheme.surface
    val muted = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.secondary
    val ink = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRoundRect(
            color = paper,
            topLeft = Offset(size.width * 0.18f, size.height * 0.13f),
            size = Size(size.width * 0.64f, size.height * 0.74f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()),
        )
        drawRoundRect(
            color = accent,
            topLeft = Offset(size.width * 0.26f, size.height * 0.24f),
            size = Size(size.width * 0.30f, 3.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
        )
        drawRoundRect(
            color = muted,
            topLeft = Offset(size.width * 0.26f, size.height * 0.39f),
            size = Size(size.width * 0.48f, 3.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
        )
        drawRoundRect(
            color = muted,
            topLeft = Offset(size.width * 0.26f, size.height * 0.51f),
            size = Size(size.width * 0.38f, 3.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
        )
        drawCircle(
            color = ink,
            radius = size.minDimension * 0.10f,
            center = Offset(size.width * 0.67f, size.height * 0.70f),
        )
    }
    Text(
        text = sourceType.take(3).uppercase(),
        color = MaterialTheme.colorScheme.inverseOnSurface,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelMedium,
    )
}
