package mx.tec.avisos.data.imagenes

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Los archivos donde escribe la cámara. La cámara es OTRA app: no puede
 * escribir en las carpetas privadas de esta. Se le presta una dirección
 * `content://` con permiso de escritura para ese único archivo — eso es lo que
 * hace `FileProvider` — y la foto queda en la carpeta de la app, no en la galería.
 *
 * En `filesDir` y no en `cacheDir`: el caché lo vacía el SISTEMA cuando a otra
 * app le falta espacio, y la que suele pedirlo es justo la cámara, al guardar.
 * Al escribir esta práctica pasó así: la foto se perdía entre el disparo y el
 * regreso. Lo de `filesDir` solo lo borra la app, y por eso existe `limpiar()`.
 */
class FotosTemporales @Inject constructor(@ApplicationContext private val context: Context) {

    // La misma carpeta que declara res/xml/rutas_de_fotos.xml. Si no coinciden, FileProvider se niega.
    private val carpeta: File get() = File(context.filesDir, "fotos").apply { mkdirs() }

    /** Un archivo vacío para la próxima foto, y la dirección que se le presta a la cámara. */
    fun nueva(): Uri {
        val archivo = File.createTempFile("foto-", ".jpg", carpeta)
        return FileProvider.getUriForFile(context, "${context.packageName}.fotos", archivo)
    }

    /** Ya se subió: las fotos originales no tienen por qué quedarse ocupando espacio. */
    fun limpiar() {
        carpeta.listFiles()?.forEach { it.delete() }
    }
}