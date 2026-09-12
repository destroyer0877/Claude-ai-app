package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassDarkBorder
import com.example.ui.theme.GlassDarkCard
import com.example.ui.theme.GlassLightBorder
import com.example.ui.theme.GlassLightCard

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    isDarkMode: Boolean = true,
    borderWidth: Dp = 1.dp,
    // For modals/dialogs (Developer Contact, PIN, API folder, Export) where the
    // animated starry background sitting behind must NOT visually mix with the
    // foreground content. Raises the opaque backing layer significantly while
    // keeping the same glass look, instead of the very light ~10-15% alpha used
    // for in-page cards.
    strong: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val cardBackground = if (isDarkMode) {
        if (strong) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xF2140509),
                    Color(0xF00E0407),
                    Color(0xF2120309)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x24FFFFFF),
                    Color(0x0FFFFFFF),
                    Color(0x1A120509)
                )
            )
        }
    } else {
        if (strong) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFFFF5F8)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xF2FFFFFF),
                    Color(0xE6FFF5F8)
                )
            )
        }
    }

    val borderColor = if (isDarkMode) {
        Brush.linearGradient(
            colors = listOf(
                Color(0x59FF8181),
                Color(0x26FFFFFF),
                Color(0x33FF2D55)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0x4DFF2D55),
                Color(0x26FF5E7E)
            )
        )
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(cardBackground)
            .border(borderWidth, borderColor, shape)
            .then(clickableModifier)
    ) {
        content()
    }
}
