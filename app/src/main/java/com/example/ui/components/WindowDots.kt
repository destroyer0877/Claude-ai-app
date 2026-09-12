package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.WindowDotGreen
import com.example.ui.theme.WindowDotRed
import com.example.ui.theme.WindowDotYellow

@Composable
fun WindowDots(
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    spacing: Dp = 8.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(WindowDotGreen)
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(WindowDotRed)
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(WindowDotYellow)
        )
    }
}
