package com.example.tesisapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID

/** Fotos de las tareas (mockup A2): se guardan en el teléfono, reducidas, hasta que la cola las envía. */
object Fotos {
    private const val MAX_LADO = 1280
    private const val CALIDAD = 75

    fun nuevoArchivo(context: Context): File =
        File(context.filesDir, "fotos").apply { mkdirs() }.let { File(it, "${UUID.randomUUID()}.jpg") }

    fun uriPara(context: Context, f: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.fotos", f)

    /** Reduce a 1280 px por lado, corrige la orientación (EXIF) y guarda en JPEG 75. Devuelve el peso en KB. */
    fun comprimir(f: File): Int {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, opts)
        var muestra = 1
        while (maxOf(opts.outWidth, opts.outHeight) / (muestra * 2) >= MAX_LADO) muestra *= 2
        val original = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = muestra }) ?: return 0
        val giro = when (ExifInterface(f.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val escala = minOf(1f, MAX_LADO.toFloat() / maxOf(original.width, original.height))
        val m = Matrix().apply { postScale(escala, escala); postRotate(giro) }
        val final = Bitmap.createBitmap(original, 0, 0, original.width, original.height, m, true)
        f.outputStream().use { final.compress(Bitmap.CompressFormat.JPEG, CALIDAD, it) }
        return (f.length() / 1024).toInt()
    }
}
