package mx.tec.avisos.data.remote

import kotlinx.serialization.Serializable

/** Lo que se manda al entrar o al registrarse. `codigoProfesor` solo al registrarse, y es opcional. */
@Serializable
data class Credenciales(
    val usuario: String,
    val password: String,
    val codigoProfesor: String? = null
)

/** Lo que el servidor devuelve a cambio: el par de tokens y quién eres. */
@Serializable
data class TokensDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val usuario: String,
    val rol: String
)

@Serializable
data class RefreshBody(val refreshToken: String)

@Serializable
data class MeDto(
    val usuario: String,
    val rol: String,
    val exp: Long,
    val sesionesActivas: Int
)

@Serializable
data class AvisoDto(
    val id: Int,
    val titulo: String,
    val cuerpo: String,
    val autor: String,
    val createdAt: String,
    /** La clave de la imagen en el servidor ("3f2a….jpg"), o null si el aviso no lleva. */
    val imagen: String? = null
)

/** `imagen`: la clave que devolvió `POST /imagenes`. Con null no se manda (explicitNulls = false). */
@Serializable
data class NuevoAvisoBody(val titulo: String, val cuerpo: String, val imagen: String? = null)

/** Lo que devuelve el servidor al recibir una imagen. */
@Serializable
data class ImagenDto(val id: String, val tipo: String, val bytes: Int)