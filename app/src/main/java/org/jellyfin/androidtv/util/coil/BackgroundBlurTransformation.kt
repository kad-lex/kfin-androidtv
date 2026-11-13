@file:Suppress("DEPRECATION")

package org.jellyfin.androidtv.util.coil

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import coil3.size.Size
import coil3.transform.Transformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
