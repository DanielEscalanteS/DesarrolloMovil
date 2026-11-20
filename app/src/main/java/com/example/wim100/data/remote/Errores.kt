
package com.example.wim100.data.remote

import com.google.gson.Gson
import java.io.IOException
import retrofit2.HttpException

data class ErrorResponseDto(
    val status: Int,
    val mensaje: String,
    val errores: Map<String, String> = emptyMap()
)

fun Throwable.mensajeParaUsuario(): String {
    if (this is HttpException) {
        val error = runCatching {
            response()?.errorBody()?.string()?.let {
                Gson().fromJson(it, ErrorResponseDto::class.java)
            }
        }.getOrNull()

        val detalles = error?.errores
            ?.values
            ?.filter { it.isNotBlank() }
            ?.joinToString("\n")

        if (!detalles.isNullOrBlank()) {
            return detalles
        }

        error?.mensaje?.takeIf { it.isNotBlank() }?.let {
            return it
        }

        return when (code()) {
            400 -> "Los datos ingresados no son válidos"
            401 -> "Usuario o contraseña incorrectos"
            404 -> "El recurso solicitado no existe"
            409 -> "Ya existe una candidata con ese correo"
            in 500..599 -> "El servidor presenta un problema"
            else -> "Error del servidor: ${code()}"
        }
    }

    return when (this) {
        is IOException -> "No se pudo conectar con el servidor. Revisa tu conexión"
        else -> message ?: "Ocurrió un error inesperado"
    }
}
