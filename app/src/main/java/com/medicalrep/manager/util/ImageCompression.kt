package com.medicalrep.manager.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object ImageCompression {
    fun compressUriToJpeg(context: Context, uri: Uri, destination: File, maxLongSide: Int, quality: Int): Boolean {
        return try {
            val resolver = context.contentResolver
            val orientation = resolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
            var sampleSize = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maxLongSide) sampleSize *= 2
            val decoded = resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            } ?: return false
            var oriented: Bitmap? = null
            var scaled: Bitmap? = null
            var rgb: Bitmap? = null
            try {
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                    ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
                }
                oriented = if (matrix.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                val source = oriented ?: decoded
                val scale = minOf(1f, maxLongSide.toFloat() / maxOf(source.width, source.height).toFloat())
                scaled = if (scale < 1f) Bitmap.createScaledBitmap(source,
                    (source.width * scale).toInt().coerceAtLeast(1),
                    (source.height * scale).toInt().coerceAtLeast(1), true) else source
                rgb = Bitmap.createBitmap(scaled!!.width, scaled!!.height, Bitmap.Config.ARGB_8888)
                Canvas(rgb!!).apply {
                    drawColor(android.graphics.Color.WHITE)
                    drawBitmap(scaled!!, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
                }
                destination.parentFile?.mkdirs()
                FileOutputStream(destination).use { output ->
                    check(rgb!!.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), output)) { "Could not encode compressed image" }
                    output.fd.sync()
                }
                destination.isFile && destination.length() > 0L
            } finally {
                if (rgb !== scaled) rgb?.recycle()
                if (scaled !== oriented && scaled !== decoded) scaled?.recycle()
                if (oriented !== decoded) oriented?.recycle()
                decoded.recycle()
            }
        } catch (_: OutOfMemoryError) {
            destination.delete()
            false
        } catch (_: Exception) {
            destination.delete()
            false
        }
    }

    fun copyUriToFile(context: Context, uri: Uri, destination: File) {
        destination.parentFile?.mkdirs()
        val input = context.contentResolver.openInputStream(uri) ?: error("Unable to read selected file")
        input.use { source -> destination.outputStream().use { output -> source.copyTo(output) } }
    }

    fun uriLength(context: Context, uri: Uri): Long? {
        if (uri.scheme == "file") return uri.path?.let(::File)?.takeIf { it.isFile }?.length()?.takeIf { it > 0L }
        if (uri.scheme == null) return File(uri.toString()).takeIf { it.isFile }?.length()?.takeIf { it > 0L }
        val queried = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else null
            }
        }.getOrNull()?.takeIf { it > 0L }
        if (queried != null) return queried
        return runCatching { context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }?.takeIf { it > 0L } }.getOrNull()
    }
}
