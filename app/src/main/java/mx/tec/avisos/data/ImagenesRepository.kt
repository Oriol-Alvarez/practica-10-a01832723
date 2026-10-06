package mx.tec.avisos.data

import android.net.Uri
import mx.tec.avisos.data.imagenes.CompresorDeImagen
import mx.tec.avisos.data.remote.AvisosApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subir una imagen es dos pasos, y ninguno es de la pantalla: reducirla en el
 * teléfono y mandarla. A cambio, el servidor devuelve una clave, y esa clave
 * —no la imagen— es lo que viaja en el aviso.
 */
@Singleton
class ImagenesRepository @Inject constructor(
    private val api: AvisosApi,
    private val compresor: CompresorDeImagen
) {

    /** Devuelve la clave que el servidor le dio ("3f2a….jpg"). */
    suspend fun subir(uri: Uri): String {
        val jpeg = compresor.comprimir(uri)
        // Una "parte" del cuerpo multipart: el nombre del campo que espera el servidor,
        // un nombre de archivo, y los bytes con su tipo.
        val parte = MultipartBody.Part.createFormData(
            "archivo",
            "aviso.jpg",
            jpeg.toRequestBody("image/jpeg".toMediaType())
        )
        return api.subirImagen(parte).id
    }
}