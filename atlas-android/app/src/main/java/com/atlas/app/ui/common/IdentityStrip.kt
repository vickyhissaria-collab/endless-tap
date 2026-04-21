package com.atlas.app.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atlas.app.ui.theme.AtlasMode
import com.atlas.app.ui.theme.Serif
import com.atlas.app.ui.theme.atlasPalette

@Composable
fun IdentityStrip(
    mode: AtlasMode,
    onModeChange: (AtlasMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = atlasPalette()
    // Scrim from canvas to transparent — mimics the CSS gradient
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to p.canvas,
                    0.6f to p.canvas.copy(alpha = 0.85f),
                    1f to p.canvas.copy(alpha = 0f)
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Brand()
            ModeSwitch(mode = mode, onModeChange = onModeChange)
        }
    }
}

@Composable
private fun Brand() {
    val p = atlasPalette()
    val transition = rememberInfiniteTransition(label = "dot")
    val dotAlpha by transition.animateFloat(
        initialValue = 1f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "dotAlpha"
    )
    val dotScale by transition.animateFloat(
        initialValue = 1f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "dotScale"
    )

    Row(verticalAlignment = Alignment.Top) {
        Text(
            "Atlas",
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = Serif,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                letterSpacing = (-0.2).sp
            ),
            color = p.text1
        )
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier
                .padding(top = 8.dp)
                .size(6.dp)
                .scale(dotScale)
                .alpha(dotAlpha)
                .clip(CircleShape)
                .background(p.accent)
        )
    }
}

@Composable
private fun ModeSwitch(mode: AtlasMode, onModeChange: (AtlasMode) -> Unit) {
    val p = atlasPalette()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(p.surface)
            .border(1.dp, p.border, RoundedCornerShape(999.dp))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModePill(
            icon = Icons.Rounded.AutoAwesome,
            label = "Ask",
            selected = mode == AtlasMode.Command,
            onClick = { onModeChange(AtlasMode.Command) }
        )
        ModePill(
            icon = Icons.Rounded.MenuBook,
            label = "Read",
            selected = mode == AtlasMode.Reading,
            onClick = { onModeChange(AtlasMode.Reading) }
        )
    }
}

@Composable
private fun ModePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val p = atlasPalette()
    val bg = if (selected) p.text1 else androidx.compose.ui.graphics.Color.Transparent
    val fg = if (selected) p.canvas else p.text3
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp,
                color = fg
            )
        }
    }
}
