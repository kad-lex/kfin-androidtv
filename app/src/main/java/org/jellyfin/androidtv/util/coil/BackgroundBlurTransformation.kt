@file:Suppress("DEPRECATION")

package org.jellyfin.androidtv.util.coil

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.size.Size
import coil3.transform.Transformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun blurImageBitmap(context: Context, imageBitmap: ImageBitmap, radius: Float = 25f): ImageBitmap {
	val androidBitmap = imageBitmap.asAndroidBitmap()
	val scale = 0.2f
	val targetWidth = (androidBitmap.width * scale).toInt().coerceAtLeast(1)
	val targetHeight = (androidBitmap.height * scale).toInt().coerceAtLeast(1)
	val small = Bitmap.createScaledBitmap(androidBitmap, targetWidth, targetHeight, true)

	val renderScript = RenderScript.create(context)
	val inputAllocation = Allocation.createFromBitmap(renderScript, small)
	val outputAllocation = Allocation.createTyped(renderScript, inputAllocation.type)
	ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript)).apply {
		setRadius(radius.coerceIn(1f, 25f))
		setInput(inputAllocation)
		forEach(outputAllocation)
	}
	outputAllocation.copyTo(small)
	renderScript.destroy()

	return small.asImageBitmap()
}

class BackgroundBlurTransformation(
	context: Context,
	private val radius: Float = 25f,
) : Transformation() {
	private val appContext = context.applicationContext

	override val cacheKey: String = "${javaClass.name}-$radius"

	override suspend fun transform(
		input: Bitmap,
		size: Size,
	): Bitmap = withContext(Dispatchers.Default) {
		// Downscale first: heavy blur is cheaper and upscale smooths the result
		val scale = 0.2f
		val targetWidth = (input.width * scale).toInt().coerceAtLeast(1)
		val targetHeight = (input.height * scale).toInt().coerceAtLeast(1)
		val small = Bitmap.createScaledBitmap(input, targetWidth, targetHeight, true)

		val renderScript = RenderScript.create(appContext)
		val inputAllocation = Allocation.createFromBitmap(renderScript, small)
		val outputAllocation = Allocation.createTyped(renderScript, inputAllocation.type)
		ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript)).apply {
			setRadius(radius.coerceIn(1f, 25f))
			setInput(inputAllocation)
			forEach(outputAllocation)
		}
		outputAllocation.copyTo(small)
		renderScript.destroy()

		small
	}
}
