package com.example.myscreenshot.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.myscreenshot.R

@Composable
fun ReminderTypeIcon(type: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.48f), RoundedCornerShape(16.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconForType(type)),
            contentDescription = "$type reminder",
            modifier = Modifier.size(34.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@DrawableRes
private fun iconForType(type: String): Int = when (type.lowercase()) {
    "travel", "hotel" -> R.drawable.icon_category_travel
    "bill" -> R.drawable.icon_category_bill
    "appointment" -> R.drawable.icon_category_appointment
    "delivery" -> R.drawable.icon_category_delivery
    "documents" -> R.drawable.icon_category_document
    else -> R.drawable.icon_category_document
}
