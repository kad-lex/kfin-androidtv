package org.jellyfin.androidtv.ui.shared

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * KFIN: dominant accent color of the lower half of the backdrop - used to
 * fill the blurred region instead of an actual blur.
 */
fun extractAccentColor(imageBitmap: ImageBitmap): Color {
	val bitmap = imageBitmap.asAndroidBitmap()
	val software = if (bitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
		bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
	} else bitmap
		?: return Color(0xFF0B0D14)

	val width = software.width
	val height = software.height
	if (width < 2 || height < 4) return Color(0xFF0B0D14)

	// sample the lower half at a coarse grid
	val stepX = max(1, width / 32)
	val stepY = max(1, (height / 2) / 16)

	// 12 hue buckets of saturated pixels; pick the dominant one
	val bucketR = LongArray(12)
	val bucketG = LongArray(12)
	val bucketB = LongArray(12)
	val bucketCount = IntArray(12)

	var y = height / 2
	while (y < height) {
		var x = 0
		while (x < width) {
			val p = software.getPixel(x, y)
			val r = android.graphics.Color.red(p)
			val g = android.graphics.Color.green(p)
			val b = android.graphics.Color.blue(p)
			val maxC = max(r, max(g, b))
			val minC = min(r, min(g, b))
			val saturation = if (maxC > 0) (maxC - minC).toFloat() / maxC else 0f
			val luminance = 0.299f * r + 0.587f * g + 0.114f * b
			if (saturation > 0.25f && luminance > 40f && luminance < 230f) {
				val hsv = FloatArray(3)
				android.graphics.Color.RGBToHSV(r, g, b, hsv)
				val bucket = ((hsv[0] / 360f) * 12).toInt().coerceIn(0, 11)
				bucketR[bucket] += r
				bucketG[bucket] += g
				bucketB[bucket] += b
				bucketCount[bucket]++
			}
			x += stepX
		}
		y += stepY
	}

	val dominant = bucketCount.indices.maxByOrNull { bucketCount[it] } ?: return Color(0xFF0B0D14)
	if (bucketCount[dominant] == 0) return Color(0xFF0B0D14)

	val count = bucketCount[dominant]
	return Color(
		(bucketR[dominant] / count).toInt().coerceIn(0, 255),
		(bucketG[dominant] / count).toInt().coerceIn(0, 255),
		(bucketB[dominant] / count).toInt().coerceIn(0, 255)
	).boostAccentSaturation()
}

private fun Color.boostAccentSaturation(): Color {
	val hsv = FloatArray(3)
	android.graphics.Color.colorToHSV(
		android.graphics.Color.argb(255, (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()),
		hsv
	)
	hsv[1] = (hsv[1] * 1.3f).coerceIn(0f, 1f)
	return Color(android.graphics.Color.HSVToColor(hsv))
}

/**
 * KFIN: ambilight-style color extraction for the card glow. Loads a small
 * version of the poster, keeps the bright (light-source) pixels and buckets
 * them into light/mid/dark groups, returning the average color of each group.
 */
suspend fun extractGlowColors(context: Context, url: String?): List<Color> {
	if (url.isNullOrEmpty()) return emptyList()

	return withContext(Dispatchers.IO) {
		runCatching {
			val request = ImageRequest.Builder(context)
				.data(url)
				.size(64, 64)
				.allowHardware(false)
				.build()
			val result = SingletonImageLoader.get(context).execute(request)
			val bitmap = result.image?.asDrawable(context.resources)
				?.let { it as? BitmapDrawable }?.bitmap
				?: return@runCatching emptyList()
			extractBrightColors(bitmap)
		}.getOrDefault(emptyList())
	}
}

private fun extractBrightColors(bitmap: Bitmap): List<Color> {
	val width = bitmap.width
	val height = bitmap.height
	val pixels = IntArray(width * height)
	bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

	// bright pixel buckets: light / mid / dim
	val rs = LongArray(3)
	val gs = LongArray(3)
	val bs = LongArray(3)
	val counts = IntArray(3)

	for (pixel in pixels) {
		val r = android.graphics.Color.red(pixel)
		val g = android.graphics.Color.green(pixel)
		val b = android.graphics.Color.blue(pixel)
		val luminance = 0.299f * r + 0.587f * g + 0.114f * b
		// skip dark areas - only light sources are used for the halo
		if (luminance < 110f) continue

		val bucket = when {
			luminance > 195f -> 0
			luminance > 145f -> 1
			else -> 2
		}
		rs[bucket] += r
		gs[bucket] += g
		bs[bucket] += b
		counts[bucket]++
	}

	return (0..2)
		.filter { counts[it] > 0 }
		.sortedByDescending { counts[it] }
		.take(2)
		.map { index ->
			val count = counts[index]
			Color(
				(rs[index] / count).toInt(),
				(gs[index] / count).toInt(),
				(bs[index] / count).toInt()
			)
		}
}

/**
 * KFIN: loads a small version of the image and extracts the lower-half accent
 * color - used for the backdrop accent fill.
 */
suspend fun extractAccentColorFromUrl(context: Context, url: String?): Color? {
	if (url.isNullOrEmpty()) return null

	return withContext(Dispatchers.IO) {
		runCatching {
			val request = ImageRequest.Builder(context)
				.data(url)
				.size(96, 96)
				.allowHardware(false)
				.build()
			val result = SingletonImageLoader.get(context).execute(request)
			val bitmap = result.image?.asDrawable(context.resources)
				?.let { it as? BitmapDrawable }?.bitmap
				?: return@runCatching null
			extractAccentColor(bitmap.asImageBitmap())
		}.getOrNull()
	}
}

/**
 * KFIN: dominant color of the bottom strip of the poster (used for the card
 * bottom lip). Saturated pixels are weighted higher; falls back to a plain
 * average when the strip is mostly grey.
 */
fun extractBottomStripColor(imageBitmap: ImageBitmap, stripHeightPx: Int = 20): Color {
	val bitmap = imageBitmap.asAndroidBitmap()
	val software = if (bitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
		bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
	} else bitmap
		?: return Color(0xE60B0D14)

	val width = software.width
	val height = software.height
	if (width < 1 || height < 1) return Color(0xE60B0D14)

	val top = (height - stripHeightPx).coerceAtLeast(0)

	var sr = 0L; var sg = 0L; var sb = 0L; var count = 0L
	var ar = 0L; var ag = 0L; var ab = 0L; var acount = 0L

	for (y in top until height step 2) {
		for (x in 0 until width step 2) {
			val p = software.getPixel(x, y)
			val r = android.graphics.Color.red(p)
			val g = android.graphics.Color.green(p)
			val b = android.graphics.Color.blue(p)
			ar += r; ag += g; ab += b; acount++
			val maxC = max(r, max(g, b))
			val minC = min(r, min(g, b))
			val saturation = if (maxC > 0) (maxC - minC).toFloat() / maxC else 0f
			if (saturation > 0.2f) {
				sr += r; sg += g; sb += b; count++
			}
		}
	}

	val color = if (count >= 4) Color((sr / count).toInt(), (sg / count).toInt(), (sb / count).toInt())
	else if (acount > 0) Color((ar / acount).toInt(), (ag / acount).toInt(), (ab / acount).toInt())
	else return Color(0xE60B0D14)

	val hsv = FloatArray(3)
	android.graphics.Color.colorToHSV(
		android.graphics.Color.argb(255, (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt()),
		hsv
	)
	hsv[1] = (hsv[1] * 1.2f).coerceIn(0f, 1f)
	hsv[2] = (hsv[2] * 0.9f).coerceIn(0f, 1f)
	return Color(android.graphics.Color.HSVToColor(hsv))
}

/**
 * KFIN: loads a small (96px) version of the poster for color extraction.
 */
suspend fun loadSmallPosterBitmap(context: Context, url: String?): Bitmap? {
	if (url.isNullOrEmpty()) return null
	return withContext(Dispatchers.IO) {
		runCatching {
			val request = ImageRequest.Builder(context)
				.data(url)
				.size(96, 96)
				.allowHardware(false)
				.build()
			val result = SingletonImageLoader.get(context).execute(request)
			result.image?.asDrawable(context.resources)?.let { it as? BitmapDrawable }?.bitmap
		}.getOrNull()
	}
}
