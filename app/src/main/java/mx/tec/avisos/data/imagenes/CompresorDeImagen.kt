package mx.tec.avisos.data.imagenes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/** La imagen no se pudo leer: no existe, no es imagen, o el permiso para leerla ya caducó. */
class ImagenIlegible : IOException("No se pudo leer la imagen")

/**
 * Convierte la foto que eligió el usuario en lo que vale la pena mandar: un
 * JPEG de 1280 px por lado, como mucho, y sin metadatos.
 *
 * Una foto de teléfono pesa de 2 a 5 MB y mide 4000 px; en una tarjeta del
 * tablón se ve a 400. Mandarla tal cual es pagar con los datos del usuario
 * —y con la memoria del teléfono que la abra— píxeles que nadie va a ver.
 */
class CompresorDeImagen @Inject constructor(@ApplicationContext private val context: Context) {

    private val resolver get() = context.contentResolver

    /** Todo en `Dispatchers.IO`: leer y decodificar 4 MB en el hilo principal congela la pantalla. */
    suspend fun comprimir(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        // 1. Solo las medidas: con inJustDecodeBounds no se carga un solo píxel.
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medidas) }
        if (medidas.outWidth <= 0) throw ImagenIlegible()

        // 2. Leerla ya reducida. inSampleSize = 2 lee uno de cada dos píxeles por lado
        //    (la cuarta parte de la memoria); se duplica mientras siga sobrando.
        var muestra = 1
        while (max(medidas.outWidth, medidas.outHeight) / (muestra * 2) >= LADO_MAX) muestra *= 2
        val leida = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra })
        } ?: throw ImagenIlegible()

        // 3. Cómo venía girada. El teléfono guarda la foto "acostada" y anota en el
        //    EXIF cuánto hay que girarla para verla derecha. BitmapFactory no lo lee.
        val orientacion = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        // 4. El tamaño exacto y el giro, en una sola pasada.
        val escala = min(1f, LADO_MAX.toFloat() / max(leida.width, leida.height))
        val matriz = Matrix().apply {
            postScale(escala, escala)
            postRotate(grados(orientacion))
        }
        val lista = Bitmap.createBitmap(leida, 0, 0, leida.width, leida.height, matriz, true)

        // 5. Un JPEG nuevo. Bitmap.compress no escribe EXIF: la ubicación, el modelo
        //    del teléfono y la fecha de la foto original no viajan al servidor.
        val salida = ByteArrayOutputStream()
        lista.compress(Bitmap.CompressFormat.JPEG, CALIDAD, salida)
        val jpeg = salida.toByteArray()

        Log.i(TAG, "${medidas.outWidth}x${medidas.outHeight}, ${kb(tamano(uri))} → ${lista.width}x${lista.height}, ${kb(jpeg.size.toLong())}")
        jpeg
    }

    /** Lo que pesa el archivo original, según quien lo comparte (la galería o la cámara). */
    private fun tamano(uri: Uri): Long =
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
        } ?: -1L

    private fun kb(bytes: Long) = if (bytes < 0) "? KB" else "${bytes / 1024} KB"

    private fun grados(orientacion: Int): Float = when (orientacion) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    private companion object {
        const val TAG = "Compresor"
        const val LADO_MAX = 1280
        const val CALIDAD = 80
    }
}