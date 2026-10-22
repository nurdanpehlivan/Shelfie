package com.shelfie.app.data.remote.gemini

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

class CompressedImage(val bytes: ByteArray, val mimeType: String)

/**
 * Shrinks a camera photo before upload: a 12 MP photo (~4 MB) becomes a ~1536 px JPEG of a few
 * hundred KB, which is plenty for item recognition, cuts upload time and token cost, and keeps
 * the request well below Gemini's inline-data size limit.
 */
@Singleton
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** @throws IOException if the image cannot be opened or decoded. */
    suspend fun compress(uri: Uri): CompressedImage = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.open(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("Unsupported or corrupt image: $uri")
        }

        // Subsample while decoding so we never hold the full-resolution bitmap in memory.
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
        }
        val sampled = resolver.open(uri).use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: throw IOException("Could not decode image: $uri")

        val exif = runCatching { resolver.open(uri).use(::ExifInterface) }.getOrNull()
        val output = sampled.scaleAndOrient(
            rotationDegrees = exif?.rotationDegrees ?: 0,
            isFlipped = exif?.isFlipped ?: false,
        )
        if (output !== sampled) sampled.recycle()

        try {
            CompressedImage(bytes = output.encodeJpeg(), mimeType = MIME_TYPE_JPEG)
        } finally {
            output.recycle()
        }
    }

    private fun ContentResolver.open(uri: Uri): InputStream =
        openInputStream(uri) ?: throw FileNotFoundException("Cannot open $uri")

    /** Largest power of two that keeps the long edge at or above [MAX_DIMENSION_PX]. */
    private fun calculateInSampleSize(width: Int, height: Int): Int {
        val longEdge = max(width, height)
        var sampleSize = 1
        while (longEdge / (sampleSize * 2) >= MAX_DIMENSION_PX) sampleSize *= 2
        return sampleSize
    }

    /** Downscales to [MAX_DIMENSION_PX] and applies EXIF orientation in a single pass. */
    private fun Bitmap.scaleAndOrient(rotationDegrees: Int, isFlipped: Boolean): Bitmap {
        val scale = min(1f, MAX_DIMENSION_PX.toFloat() / max(width, height))
        if (scale == 1f && rotationDegrees == 0 && !isFlipped) return this

        val matrix = Matrix().apply {
            postScale(scale, scale)
            if (isFlipped) postScale(-1f, 1f)
            if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    /** Steps quality down until the output fits [MAX_OUTPUT_BYTES] (or quality bottoms out). */
    private fun Bitmap.encodeJpeg(): ByteArray {
        var quality = INITIAL_JPEG_QUALITY
        while (true) {
            val bytes = ByteArrayOutputStream().use { stream ->
                compress(Bitmap.CompressFormat.JPEG, quality, stream)
                stream.toByteArray()
            }
            if (bytes.size <= MAX_OUTPUT_BYTES || quality <= MIN_JPEG_QUALITY) return bytes
            quality -= JPEG_QUALITY_STEP
        }
    }

    private companion object {
        const val MIME_TYPE_JPEG = "image/jpeg"
        const val MAX_DIMENSION_PX = 1536
        const val MAX_OUTPUT_BYTES = 1_000_000
        const val INITIAL_JPEG_QUALITY = 85
        const val MIN_JPEG_QUALITY = 55
        const val JPEG_QUALITY_STEP = 10
    }
}
