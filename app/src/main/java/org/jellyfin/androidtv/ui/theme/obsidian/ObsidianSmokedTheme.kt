package org.jellyfin.androidtv.ui.theme.obsidian

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.base.Text

/**
 * ============================================================================
 * ТЕМА №3: OBSIDIAN SMOKED GLASS (OLED THEATER) ДЛЯ JELLYFIN ANDROID TV
 * ============================================================================
 */
object ObsidianColors {
    val OledBackground = Color(0xFF050608)
    val SmokedSurface = Color(0xC70D0F15)       // ~78% прозрачности
    val SmokedPanelElevated = Color(0xE00A0C10) // ~88% для OSD-плеера и модалок
    val RimLightTop = Color(0x3DFFFFFF)         // Верхний зеркальный блик (24% white)
    val RimBorderBase = Color(0x14FFFFFF)       // Базовый контур стекла (8% white)
    val RimBorderFocused = Color(0x73FFFFFF)    // Яркий контур при фокусе с пульта (45% white)
    val YouTubeRed = Color(0xFFFF0033)          // Красный ползунок плеера
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFFA1A1AA)
    val AccentGold = Color(0xFFFBBF24)
}

/**
 * Базовый модификатор дымчатого стекла с верхним световым бликом (Rim Light)
 */
fun Modifier.obsidianGlass(
    cornerRadius: Dp = 20.dp,
    isFocused: Boolean = false,
    elevated: Boolean = false
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val bgColor = if (elevated) ObsidianColors.SmokedPanelElevated else ObsidianColors.SmokedSurface

    val borderBrush = Brush.verticalGradient(
        colors = if (isFocused) {
            listOf(Color.White.copy(alpha = 0.65f), ObsidianColors.RimBorderFocused)
        } else {
            listOf(ObsidianColors.RimLightTop, ObsidianColors.RimBorderBase)
        }
    )

    return this
        .clip(shape)
        .background(bgColor)
        .border(
            border = BorderStroke(width = if (isFocused) 1.5.dp else 1.dp, brush = borderBrush),
            shape = shape
        )
}

/**
 * Карточка медиа для ряда "Продолжить просмотр" (Android TV D-Pad Ready)
 * - isSeries = false -> Вертикальный постер 2:3 (ширина 164.dp)
 * - isSeries = true  -> Широкоформатный кадр серии 16:9 (ширина 320.dp)
 */
@Composable
fun ObsidianContinueWatchingCard(
    title: String,
    subtitle: String,
    remainingText: String,
    progress: Float,
    isSeries: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkContent: @Composable BoxScope.() -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "tvFocusScale"
    )

    val cardWidth = if (isSeries) 320.dp else 164.dp

    Column(
        modifier = modifier
            .width(cardWidth)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .obsidianGlass(cornerRadius = 20.dp, isFocused = isFocused)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            artworkContent()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 120f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = subtitle.uppercase(),
                    color = ObsidianColors.TextSecondary,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = title,
                    color = ObsidianColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = ObsidianColors.TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "ост. $remainingText",
                    color = ObsidianColors.TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(ObsidianColors.YouTubeRed)
                )
            }
        }
    }
}

/**
 * Плавающая консоль видеоплеера (OSD Overlay) с красным ползунком в стиле YouTube
 */
@Composable
fun ObsidianPlayerOsdOverlay(
    title: String,
    streamInfo: String,
    currentTime: String,
    remainingTime: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth(0.88f)
            .obsidianGlass(cornerRadius = 26.dp, elevated = true)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = ObsidianColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = streamInfo,
                color = ObsidianColors.TextSecondary,
                fontSize = 12.sp
            )
        }

        // Красный прогресс-бар в стиле YouTube (#FF0033) без сторонних библиотек
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(ObsidianColors.YouTubeRed)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = currentTime, color = ObsidianColors.TextSecondary, fontSize = 12.sp)
            Text(text = remainingTime, color = ObsidianColors.TextSecondary, fontSize = 12.sp)
        }
    }
}
