package com.example.myscreenshot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

@Composable
fun ReminderTypeIcon(type: String, modifier: Modifier = Modifier) {
    val colors = IconPalette(
        ink = MaterialTheme.colorScheme.onSurface,
        surface = MaterialTheme.colorScheme.surface,
        line = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
        accent = MaterialTheme.colorScheme.secondary,
        proof = MaterialTheme.colorScheme.primary,
        warm = MaterialTheme.colorScheme.tertiary,
    )
    Box(
        modifier = modifier
            .size(54.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(17.dp)),
    ) {
        Canvas(modifier = Modifier.size(54.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = colors.proof.copy(alpha = 0.10f),
                topLeft = Offset(w * 0.12f, h * 0.12f),
                size = Size(w * 0.76f, h * 0.76f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.24f, h * 0.24f),
            )
            when (type.normalizedType()) {
                "travel" -> drawTravelGlyph(colors)
                "hotel" -> drawHotelGlyph(colors)
                "bill" -> drawBillGlyph(colors)
                "appointment" -> drawAppointmentGlyph(colors)
                "delivery" -> drawDeliveryGlyph(colors)
                else -> drawDocumentGlyph(colors)
            }
        }
    }
}

private data class IconPalette(
    val ink: Color,
    val surface: Color,
    val line: Color,
    val accent: Color,
    val proof: Color,
    val warm: Color,
)

private fun String.normalizedType(): String = when (lowercase()) {
    "flight", "travel" -> "travel"
    "hotel" -> "hotel"
    "bill", "payment" -> "bill"
    "appointment" -> "appointment"
    "delivery", "package" -> "delivery"
    else -> "document"
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTravelGlyph(colors: IconPalette) {
    val stroke = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawLine(colors.proof, Offset(size.width * 0.18f, size.height * 0.65f), Offset(size.width * 0.82f, size.height * 0.34f), 2.dp.toPx(), StrokeCap.Round)
    rotate(-24f, pivot = Offset(size.width * 0.50f, size.height * 0.50f)) {
        val plane = Path().apply {
            moveTo(size.width * 0.23f, size.height * 0.47f)
            lineTo(size.width * 0.76f, size.height * 0.47f)
            lineTo(size.width * 0.86f, size.height * 0.53f)
            lineTo(size.width * 0.76f, size.height * 0.59f)
            lineTo(size.width * 0.23f, size.height * 0.59f)
            lineTo(size.width * 0.38f, size.height * 0.53f)
            close()
        }
        drawPath(plane, colors.ink)
        drawLine(colors.accent, Offset(size.width * 0.39f, size.height * 0.39f), Offset(size.width * 0.55f, size.height * 0.53f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        drawLine(colors.accent, Offset(size.width * 0.39f, size.height * 0.67f), Offset(size.width * 0.55f, size.height * 0.53f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
    }
    drawCircle(colors.warm, radius = 2.4.dp.toPx(), center = Offset(size.width * 0.78f, size.height * 0.22f))
    drawLine(colors.proof.copy(alpha = 0.45f), Offset(size.width * 0.19f, size.height * 0.73f), Offset(size.width * 0.35f, size.height * 0.66f), stroke.width, StrokeCap.Round)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHotelGlyph(colors: IconPalette) {
    val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawRoundRect(colors.ink, Offset(size.width * 0.22f, size.height * 0.23f), Size(size.width * 0.56f, size.height * 0.54f), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
    drawRoundRect(colors.surface, Offset(size.width * 0.29f, size.height * 0.31f), Size(size.width * 0.15f, size.height * 0.14f), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
    drawRoundRect(colors.surface, Offset(size.width * 0.56f, size.height * 0.31f), Size(size.width * 0.15f, size.height * 0.14f), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
    drawLine(colors.accent, Offset(size.width * 0.22f, size.height * 0.58f), Offset(size.width * 0.78f, size.height * 0.58f), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
    drawArc(colors.proof, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(size.width * 0.38f, size.height * 0.47f), size = Size(size.width * 0.24f, size.height * 0.30f), style = stroke)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAppointmentGlyph(colors: IconPalette) {
    drawRoundRect(colors.ink, Offset(size.width * 0.22f, size.height * 0.20f), Size(size.width * 0.56f, size.height * 0.58f), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
    drawRoundRect(colors.surface, Offset(size.width * 0.28f, size.height * 0.33f), Size(size.width * 0.44f, size.height * 0.36f), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
    drawLine(colors.accent, Offset(size.width * 0.30f, size.height * 0.43f), Offset(size.width * 0.70f, size.height * 0.43f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(colors.line.copy(alpha = 0.8f), Offset(size.width * 0.30f, size.height * 0.53f), Offset(size.width * 0.70f, size.height * 0.53f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(colors.proof, Offset(size.width * 0.39f, size.height * 0.61f), Offset(size.width * 0.46f, size.height * 0.67f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
    drawLine(colors.proof, Offset(size.width * 0.46f, size.height * 0.67f), Offset(size.width * 0.62f, size.height * 0.55f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(colors.warm, 2.3.dp.toPx(), Offset(size.width * 0.34f, size.height * 0.19f))
    drawCircle(colors.warm, 2.3.dp.toPx(), Offset(size.width * 0.66f, size.height * 0.19f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBillGlyph(colors: IconPalette) {
    drawRoundRect(colors.ink, Offset(size.width * 0.16f, size.height * 0.28f), Size(size.width * 0.68f, size.height * 0.44f), androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()))
    drawRoundRect(colors.proof, Offset(size.width * 0.23f, size.height * 0.39f), Size(size.width * 0.18f, size.height * 0.13f), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
    drawLine(colors.surface, Offset(size.width * 0.49f, size.height * 0.43f), Offset(size.width * 0.74f, size.height * 0.43f), strokeWidth = 2.4.dp.toPx(), cap = StrokeCap.Round)
    drawLine(colors.surface.copy(alpha = 0.65f), Offset(size.width * 0.23f, size.height * 0.61f), Offset(size.width * 0.52f, size.height * 0.61f), strokeWidth = 2.4.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(colors.accent, 5.dp.toPx(), Offset(size.width * 0.65f, size.height * 0.62f))
    drawCircle(colors.warm, 5.dp.toPx(), Offset(size.width * 0.75f, size.height * 0.62f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDocumentGlyph(colors: IconPalette) {
    val page = Path().apply {
        moveTo(size.width * 0.28f, size.height * 0.15f)
        lineTo(size.width * 0.60f, size.height * 0.15f)
        lineTo(size.width * 0.76f, size.height * 0.31f)
        lineTo(size.width * 0.76f, size.height * 0.82f)
        quadraticTo(size.width * 0.76f, size.height * 0.88f, size.width * 0.70f, size.height * 0.88f)
        lineTo(size.width * 0.28f, size.height * 0.88f)
        quadraticTo(size.width * 0.22f, size.height * 0.88f, size.width * 0.22f, size.height * 0.82f)
        lineTo(size.width * 0.22f, size.height * 0.21f)
        quadraticTo(size.width * 0.22f, size.height * 0.15f, size.width * 0.28f, size.height * 0.15f)
        close()
    }
    drawPath(page, colors.ink)
    drawPath(Path().apply {
        moveTo(size.width * 0.60f, size.height * 0.15f)
        lineTo(size.width * 0.60f, size.height * 0.32f)
        lineTo(size.width * 0.76f, size.height * 0.32f)
        close()
    }, colors.accent)
    drawLine(colors.surface, Offset(size.width * 0.32f, size.height * 0.47f), Offset(size.width * 0.64f, size.height * 0.47f), 2.6.dp.toPx(), StrokeCap.Round)
    drawLine(colors.surface.copy(alpha = 0.7f), Offset(size.width * 0.32f, size.height * 0.58f), Offset(size.width * 0.58f, size.height * 0.58f), 2.6.dp.toPx(), StrokeCap.Round)
    drawCircle(colors.proof, 4.dp.toPx(), Offset(size.width * 0.36f, size.height * 0.74f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDeliveryGlyph(colors: IconPalette) {
    val top = Path().apply {
        moveTo(size.width * 0.50f, size.height * 0.17f)
        lineTo(size.width * 0.78f, size.height * 0.32f)
        lineTo(size.width * 0.50f, size.height * 0.47f)
        lineTo(size.width * 0.22f, size.height * 0.32f)
        close()
    }
    val left = Path().apply {
        moveTo(size.width * 0.22f, size.height * 0.32f)
        lineTo(size.width * 0.50f, size.height * 0.47f)
        lineTo(size.width * 0.50f, size.height * 0.82f)
        lineTo(size.width * 0.22f, size.height * 0.66f)
        close()
    }
    val right = Path().apply {
        moveTo(size.width * 0.78f, size.height * 0.32f)
        lineTo(size.width * 0.50f, size.height * 0.47f)
        lineTo(size.width * 0.50f, size.height * 0.82f)
        lineTo(size.width * 0.78f, size.height * 0.66f)
        close()
    }
    drawPath(top, colors.accent)
    drawPath(left, colors.ink)
    drawPath(right, colors.warm)
    drawLine(colors.surface.copy(alpha = 0.8f), Offset(size.width * 0.34f, size.height * 0.25f), Offset(size.width * 0.63f, size.height * 0.40f), 3.dp.toPx(), StrokeCap.Round)
    drawRoundRect(colors.proof, Offset(size.width * 0.61f, size.height * 0.58f), Size(size.width * 0.21f, size.height * 0.12f), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
}
