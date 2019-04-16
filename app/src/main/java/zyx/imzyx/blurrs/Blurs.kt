package zyx.imzyx.blurrs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.lang.NullPointerException

/**
 * Blur with render script
 * Created by holmes on 19-4-16.
 */
class Blurs(val context: Context) {

    private var renderScript: RenderScript? = null
    private var blurScript: ScriptIntrinsicBlur? = null

    private var radius: Float = 8f
    private var sample: Int = 1

    @Synchronized
    fun initRender() {
        if (renderScript == null) {
            renderScript = RenderScript.create(context)
            blurScript = ScriptIntrinsicBlur.create(renderScript!!, Element.U8_4(renderScript!!))
            setRadius(radius)
        }
    }

    fun setRadius(radius: Float) {
        val r = when {
            radius <= 0f -> 1f
            radius > 25f -> 25f
            else -> radius
        }
        this.radius = r
        blurScript?.setRadius(r)
    }

    fun setSample(sample: Int) {
        this.sample = sample
    }

    suspend fun guessSample(src: String, dstWidth: Int, dstHeight: Int): Int {
        return withContext(Dispatchers.IO) {
            val fromOpt = BitmapFactory.Options()
            fromOpt.inJustDecodeBounds = true
            BitmapFactory.decodeFile(src, fromOpt)

            computeSampleSize(fromOpt, dstWidth, dstHeight)
        }
    }

    suspend fun dumpPicSize(src: String): BitmapFactory.Options {
        return withContext(Dispatchers.IO) {
            val fromOpt = BitmapFactory.Options()
            fromOpt.inJustDecodeBounds = true
            BitmapFactory.decodeFile(src, fromOpt)
            return@withContext fromOpt
        }
    }

    suspend fun blur(src: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            val fromOpt = BitmapFactory.Options()
            val sample = this@Blurs.sample
            fromOpt.inSampleSize = sample
            fromOpt.inJustDecodeBounds = false
            val fromBitmap = try {
                BitmapFactory.decodeFile(src, fromOpt)
            } catch (e: Exception) {
                null
            }

            if (fromBitmap == null) {
                throw NullPointerException("can not read source bitmap file. $src")
            }

            // create renderScript
            try {
                if (!isActive) {
                    return@withContext null
                }

                val input = Allocation.createFromBitmap(
                    renderScript, fromBitmap,
                    Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT
                )
                val output = Allocation.createTyped(renderScript, input.type)

                val blurBitmap = Bitmap.createBitmap(fromBitmap.width, fromBitmap.height, Bitmap.Config.ARGB_8888)
                if (!isActive) {
                    blurBitmap.recycle()
                    return@withContext null
                }

                input.copyFrom(fromBitmap)
                blurScript!!.setInput(input)
                blurScript!!.forEach(output)
                output.copyTo(blurBitmap)

                //blurBitmap.recycle()
                if (!isActive) {
                    blurBitmap.recycle()
                    null
                } else {
                    blurBitmap
                }
            } catch (e: Exception) {
                // e.printStackTrace()
                throw e
            } finally {
                try {
                    fromBitmap.recycle()
                } catch (e: Exception) {
                }
            }
        }
    }

    @Synchronized
    fun destroy() {
        renderScript?.also {
            it.destroy()
        }
        blurScript?.destroy()
        renderScript = null
        blurScript = null
    }

    fun computeSampleSize(srcOpt: BitmapFactory.Options, dstWidth: Int, dstHeight: Int): Int {
        return computeSampleSize(srcOpt.outWidth, srcOpt.outHeight, dstWidth, dstHeight)
    }

    fun computeSampleSize(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): Int {
        var expectSample = 1
        while (srcWidth / expectSample > dstWidth) {
            expectSample++
        }
        while (srcHeight / expectSample > dstHeight) {
            expectSample++
        }
        if (expectSample > 1) {
            if ((expectSample and 0x1) != 0) {
                // up to nearest power of 2.
                expectSample++
            }
        }
        return expectSample
    }

}