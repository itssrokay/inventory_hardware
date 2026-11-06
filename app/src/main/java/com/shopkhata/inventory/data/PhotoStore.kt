package com.shopkhata.inventory.data

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class PhotoStore(private val context: Context) {
    suspend fun load(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= 28) ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val scale = 1600.0 / maxOf(info.size.width, info.size.height).coerceAtLeast(1600)
            decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Choose a valid image." }
            val options = BitmapFactory.Options().apply {
                inSampleSize = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > 1800) inSampleSize *= 2
            }
            requireNotNull(context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) })
        }
    }
    suspend fun import(uri: Uri): String = save(load(uri))
    /** Consumes the bitmap after writing the reviewed crop, never the original photo. */
    suspend fun save(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val relative = "photos/${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, relative).apply { parentFile?.mkdirs() }
        val factor = minOf(1.0, 1600.0 / maxOf(bitmap.width, bitmap.height))
        val output = Bitmap.createBitmap((bitmap.width * factor).toInt().coerceAtLeast(1), (bitmap.height * factor).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        Canvas(output).apply { drawColor(Color.WHITE); scale(factor.toFloat(), factor.toFloat()); drawBitmap(bitmap, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG)) }
        try {
            file.outputStream().use { check(output.compress(Bitmap.CompressFormat.JPEG, 85, it)) }
            require(file.length() <= 2_000_000) { "Photo is too large. Crop a smaller area." }
            relative
        } catch (error: Exception) { file.delete(); throw error }
        finally { output.recycle(); bitmap.recycle() }
    }
}
