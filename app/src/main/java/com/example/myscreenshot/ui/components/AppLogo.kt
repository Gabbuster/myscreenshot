package com.example.myscreenshot.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.myscreenshot.R

@Composable
fun AppLogo(modifier: Modifier = Modifier.size(72.dp)) {
    Image(
        painter = painterResource(R.drawable.screenshot_reminder_logo),
        contentDescription = "Snappy, the Screen4U fox mascot",
        modifier = modifier,
    )
}
