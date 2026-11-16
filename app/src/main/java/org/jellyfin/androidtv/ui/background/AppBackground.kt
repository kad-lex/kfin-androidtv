package org.jellyfin.androidtv.ui.background

import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.shared.extractAccentColor
import org.koin.compose.koinInject

@Composable
private fun AppThemeBackground() {
	val context = LocalContext.current
	val themeBackground = remember(context.theme) {
		val attributes = context.theme.obtainStyledAttributes(intArrayOf(R.attr.defaultBackground))
		val drawable = attributes.getDrawable(0)
		attributes.recycle()

		if (drawable is ColorDrawable) drawable.toBitmap(1, 1).asImageBitmap()
		else drawable?.toBitmap()?.asImageBitmap()
	}

	if (themeBackground != null) {
		Image(
			bitmap = themeBackground,
			contentDescription = null,
			alignment = Alignment.Center,
			contentScale = ContentScale.Crop,
			modifier = Modifier.fillMaxSize()
		)
	} else {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(Color.Black)
		)
	}
}

@Composable
fun AppBackground() {
	val backgroundService = koinInject<BackgroundService>()
	val currentBackground by backgroundService.currentBackground.collectAsState()
	val enabled by backgroundService.enabled.collectAsState()

	if (enabled) {
		AnimatedContent(
			targetState = currentBackground,
			transitionSpec = {
				val duration = (BackgroundService.TRANSITION_DURATION.inWholeMilliseconds / 2).toInt()
				fadeIn(tween(durationMillis = duration)) togetherWith fadeOut(snap(delayMillis = duration))
			},
			label = "BackgroundTransition",
		) { background ->
			if (background != null) {
				BackdropWithBlurBoundary(background)
			} else {
				AppThemeBackground()
			}
		}
	}
}

// Fraction of screen height above which the backdrop stays sharp;
// below the boundary it is blurred and darkened (matches the grid top at 55%).
private const val BACKDROP_BLUR_BOUNDARY = 0.45f
private const val BACKDROP_BLUR_FADE = 0.08f

@Composable
private fun BackdropWithBlurBoundary(bitmap: ImageBitmap) {
	val context = LocalContext.current
	val tint = ColorFilter.tint(colorResource(R.color.background_filter), BlendMode.SrcAtop)
	// KFIN: accent color of the lower half, extracted off the main thread -
	// it replaces the blur below the boundary
	var accent by remember(bitmap, context) { mutableStateOf<Color?>(null) }
	LaunchedEffect(bitmap, context) {
		accent = withContext(Dispatchers.Default) { extractAccentColor(bitmap) }
	}

	BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
		val boundary = BACKDROP_BLUR_BOUNDARY
		val fadeEnd = (BACKDROP_BLUR_BOUNDARY + BACKDROP_BLUR_FADE).coerceAtMost(1f)

		// Sharp backdrop
		Image(
			bitmap = bitmap,
			contentDescription = null,
			alignment = Alignment.Center,
			contentScale = ContentScale.Crop,
			colorFilter = tint,
			modifier = Modifier.fillMaxSize()
		)

		// Accent fill, masked to the region below the boundary
		Box(
			modifier = Modifier
				.fillMaxSize()
				.graphicsLayer { alpha = 0.99f }
		) {
			// KFIN: 80% accent fill instead of blur, gradually turning black
			// towards the bottom edge
			val accentFill = accent ?: Color(0xFF0B0D14)
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(
						Brush.verticalGradient(
							0f to accentFill.copy(alpha = 0f),
							boundary to accentFill.copy(alpha = 0.7f),
							fadeEnd to accentFill.copy(alpha = 0.7f),
							1f to androidx.compose.ui.graphics.lerp(accentFill.copy(alpha = 0.7f), Color.Black, 0.85f)
						)
					)
			)
		}

		// Darkening below the boundary, same geometry
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(
					Brush.verticalGradient(
						0f to Color.Transparent,
						boundary to Color.Transparent,
						fadeEnd to Color.Black.copy(alpha = 0.3f),
						1f to Color.Black.copy(alpha = 0.3f)
					)
				)
		)
	}
}
