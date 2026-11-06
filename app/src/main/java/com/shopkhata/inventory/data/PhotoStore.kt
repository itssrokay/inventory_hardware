package com.shopkhata.inventory.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class PhotoStore(private val context: Context) {
    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = if (Build.VERSION.SDK_INT >= 28) ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val scale = 1000.0 / maxOf(info.size.width, info.size.height).coerceAtLeast(1000)
            decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0) { "Choose a valid image." }
            val options = BitmapFactory.Options().apply {
                inSampleSize = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > 1400) inSampleSize *= 2
            }
            requireNotNull(context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) })
        }
        val relative = "photos/${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, relative).apply { parentFile?.mkdirs() }
        try {
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 78, it)) }
            require(file.length() <= 2_000_000) { "Photo is too large. Choose a smaller image." }
            relative
        } catch (error: Exception) { file.delete(); throw error }
        finally { bitmap.recycle() }
    }
}
