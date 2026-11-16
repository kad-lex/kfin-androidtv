package org.jellyfin.androidtv.util.coil

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.size.Size
import coil3.transform.Transformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * KFIN: pure-Kotlin multi-pass box blur. Replaces RenderScript, which is
 * deprecated and crashes (RSInvalidStateException) on Android 11/12 devices
 * like the NVIDIA Shield.
 */
private fun boxBlur(bitmap: Bitmap, radius: Int, passes: Int = 3) {
	val width = bitmap.width
	val height = bitmap.height
	if (width < 3 || height < 3 || radius < 1) return

	val pixels = IntArray(width * height)
	bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
	val tmp = IntArray(width * height)
	val window = radius * 2 + 1

	repeat(passes) {
		// horizontal pass: pixels -> tmp
		for (y in 0 until height) {
			val row = y * width
			var sr = 0; var sg = 0; var sb = 0
			for (x in -radius..radius) {
				val p = pixels[row + x.coerceIn(0, width - 1)]
				sr += p shr 16 and 0xFF
				sg += p shr 8 and 0xFF
				sb += p and 0xFF
			}
			for (x in 0 until width) {
				tmp[row + x] = (0xFF shl 24) or
					((sr / window) shl 16) or
					((sg / window) shl 8) or
					(sb / window)
				val remove = pixels[row + (x - radius).coerceIn(0, width - 1)]
				val add = pixels[row + (x + radius + 1).coerceIn(0, width - 1)]
				sr += (add shr 16 and 0xFF) - (remove shr 16 and 0xFF)
				sg += (add shr 8 and 0xFF) - (remove shr 8 and 0xFF)
				sb += (add and 0xFF) - (remove and 0xFF)
			}
		}

		// vertical pass: tmp -> pixels
		for (x in 0 until width) {
			var sr = 0; var sg = 0; var sb = 0
			for (y in -radius..radius) {
				val p = tmp[y.coerceIn(0, height - 1) * width + x]
				sr += p shr 16 and 0xFF
				sg += p shr 8 and 0xFF
				sb += p and 0xFF
			}
			for (y in 0 until height) {
				val index = y * width + x
				pixels[index] = (0xFF shl 24) or
					((sr / window) shl 16) or
					((sg / window) shl 8) or
					(sb / window)
				val remove = tmp[(y - radius).coerceIn(0, height - 1) * width + x]
				val add = tmp[(y + radius + 1).coerceIn(0, height - 1) * width + x]
				sr += (add shr 16 and 0xFF) - (remove shr 16 and 0xFF)
				sg += (add shr 8 and 0xFF) - (remove shr 8 and 0xFF)
				sb += (add and 0xFF) - (remove and 0xFF)
			}
		}
	}

	bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
}

private fun blurSmall(small: Bitmap, radius: Float) {
	val boxRadius = ((radius.coerceIn(1f, 25f)) / 2f).toInt().coerceAtLeast(1)
	boxBlur(small, boxRadius, passes = 3)
}

// KFIN: HARDWARE bitmaps do not allow pixel access - copy() supports
// converting from HARDWARE since API 26
private fun toSoftware(bitmap: Bitmap): Bitmap {
	if (bitmap.config != Bitmap.Config.HARDWARE) return bitmap
	return bitmap.copy(Bitmap.Config.ARGB_8888, false)
		?: Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
}

fun blurImageBitmap(context: android.content.Context, imageBitmap: ImageBitmap, radius: Float = 25f): ImageBitmap {
	val androidBitmap = toSoftware(imageBitmap.asAndroidBitmap())
	val scale = 0.2f
	val targetWidth = (androidBitmap.width * scale).toInt().coerceAtLeast(1)
	val targetHeight = (androidBitmap.height * scale).toInt().coerceAtLeast(1)
	val small = Bitmap.createScaledBitmap(androidBitmap, targetWidth, targetHeight, true)

	blurSmall(small, radius)

	return small.asImageBitmap()
}

class BackgroundBlurTransformation(
	context: android.content.Context,
	private val radius: Float = 25f,
) : Transformation() {
	override val cacheKey: String = "${javaClass.name}-$radius"

	override suspend fun transform(
		input: Bitmap,
		size: Size,
	): Bitmap = withContext(Dispatchers.Default) {
		// Downscale first: heavy blur is cheaper and upscale smooths the result
		val source = toSoftware(input)
		val scale = 0.2f
		val targetWidth = (source.width * scale).toInt().coerceAtLeast(1)
		val targetHeight = (source.height * scale).toInt().coerceAtLeast(1)
		val small = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)

		blurSmall(small, radius)

		small
	}
}
