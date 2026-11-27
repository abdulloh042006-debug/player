package com.shaxsiy.player.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shaxsiy.player.ui.theme.DeepSpaceBg
import com.shaxsiy.player.ui.theme.GlassBorderBright
import com.shaxsiy.player.ui.theme.GlassBorderSoft
import com.shaxsiy.player.ui.theme.GlassFillLight
import com.shaxsiy.player.ui.theme.GlassFillMedium
import com.shaxsiy.player.ui.theme.NeonCyan
import com.shaxsiy.player.ui.theme.NeonMagenta
import com.shaxsiy.player.ui.theme.NeonViolet
import com.shaxsiy.player.ui.theme.TextPrimary
import com.shaxsiy.player.ui.theme.TextSecondary

/**
 * iPhone uslubidagi prujinali (spring) bosilish animatsiyasi.
 */
fun Modifier.iosBounceClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ios_bounce_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            while (true) {
                awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
        .clickable(
            enabled = enabled,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
}

/**
 * Orqa fonda silliq suzib yuruvchi neon cyan/magenta/violet shisha nurlari.
 * Musiqa chalinganda nurlar jonliroq nafas oladi.
 */
@Composable
fun GlassAmbientBackground(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "ambient_mesh")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_drift"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 2400 else 6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpaceBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Yuqori chap Magenta/Pink nur
            val magentaCenter = Offset(
                x = w * (0.12f + 0.16f * drift),
                y = h * (0.14f + 0.08f * (1f - drift))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonMagenta.copy(alpha = if (isPlaying) 0.34f else 0.22f),
                        NeonMagenta.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = magentaCenter,
                    radius = w * 0.72f * pulse
                ),
                radius = w * 0.72f * pulse,
                center = magentaCenter
            )

            // O‘rta o‘ng Neon Cyan nur
            val cyanCenter = Offset(
                x = w * (0.88f - 0.14f * drift),
                y = h * (0.48f + 0.10f * drift)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = if (isPlaying) 0.30f else 0.18f),
                        NeonCyan.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = cyanCenter,
                    radius = w * 0.68f * pulse
                ),
                radius = w * 0.68f * pulse,
                center = cyanCenter
            )

            // Pastki chap Binafsha (Violet) nur
            val violetCenter = Offset(
                x = w * (0.25f + 0.20f * (1f - drift)),
                y = h * (0.86f - 0.06f * drift)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonViolet.copy(alpha = if (isPlaying) 0.36f else 0.24f),
                        NeonViolet.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = violetCenter,
                    radius = w * 0.70f
                ),
                radius = w * 0.70f,
                center = violetCenter
            )
        }

        content()
    }
}

/**
 * Muzli shisha (Frosted Glassmorphism) kartasi.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    isHighlighted: Boolean = false,
    accentColor: Color = NeonCyan,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val fillBrush = if (isHighlighted) {
        Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.24f),
                GlassFillLight,
                NeonMagenta.copy(alpha = 0.14f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                GlassFillLight,
                GlassFillMedium,
                Color(0x0AFFFFFF)
            )
        )
    }

    val borderBrush = if (isHighlighted) {
        Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.75f),
                NeonMagenta.copy(alpha = 0.45f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                GlassBorderBright,
                GlassBorderSoft
            )
        )
    }

    val baseMod = modifier
        .clip(shape)
        .background(fillBrush)
        .border(width = 1.dp, brush = borderBrush, shape = shape)

    val finalMod = if (onClick != null) {
        baseMod.iosBounceClickable(onClick = onClick)
    } else {
        baseMod
    }

    Box(modifier = finalMod, content = content)
}

/**
 * iOS uslubidagi yarim shaffof shisha kapsula tugma (-10s, +10s, Shuffle, Play va h.k.).
 */
@Composable
fun GlassPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isGlowing: Boolean = false,
    glowColor: Color = NeonCyan,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val bgBrush = if (isGlowing) {
        Brush.horizontalGradient(
            colors = listOf(
                glowColor.copy(alpha = 0.45f),
                NeonMagenta.copy(alpha = 0.35f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.06f)
            )
        )
    }

    val borderBrush = if (isGlowing) {
        Brush.horizontalGradient(
            colors = listOf(
                glowColor.copy(alpha = 0.90f),
                Color.White.copy(alpha = 0.55f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, borderBrush, shape)
            .iosBounceClickable(scaleDown = 0.92f, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Yumaloq shisha ikona tugmasi.
 */
@Composable
fun GlassCircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isGlowing: Boolean = false,
    glowColor: Color = NeonCyan,
    content: @Composable BoxScope.() -> Unit
) {
    GlassPillButton(
        onClick = onClick,
        modifier = modifier.size(size),
        isGlowing = isGlowing,
        glowColor = glowColor,
        shape = CircleShape,
        content = content
    )
}

/**
 * iPhone uslubidagi muzli shisha qidiruv satri.
 */
@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    accentColor: Color = NeonCyan
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(accentColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tozalash",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}