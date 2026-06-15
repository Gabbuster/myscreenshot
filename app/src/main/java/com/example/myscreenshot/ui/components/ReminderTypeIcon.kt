package com.example.myscreenshot.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.myscreenshot.R

@Composable
fun ReminderTypeIcon(type: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.62f), RoundedCornerShape(8.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(type.iconRes()),
            contentDescription = "${type.cleanTypeName()} icon",
            modifier = Modifier.size(38.dp),
        )
    }
}

private fun String.iconRes(): Int = when (lowercase()) {
    "flight", "travel" -> R.drawable.ic_premium_travel
    "hotel" -> R.drawable.ic_premium_hotel
    "bill", "payment" -> R.drawable.ic_premium_bill
    "appointment" -> R.drawable.ic_premium_appointment
    "delivery", "package" -> R.drawable.ic_premium_delivery
    else -> R.drawable.ic_premium_document
}

private fun String.cleanTypeName(): String = when (lowercase()) {
    "documents" -> "Document"
    else -> replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
