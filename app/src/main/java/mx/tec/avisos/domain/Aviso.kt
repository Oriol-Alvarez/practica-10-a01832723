package mx.tec.avisos.domain

/** Un aviso del tablón. Lo escribe un profesor; lo lee cualquiera con sesión. */
data class Aviso(
    val id: Int,
    val titulo: String,
    val cuerpo: String,
    val autor: String,
    val creadoEn: String,
    /** De dónde bajar la imagen, o null si el aviso no lleva. */
    val imagenUrl: String? = null
)