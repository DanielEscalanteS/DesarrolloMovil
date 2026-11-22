package com.example.wim100.data.remote

data class LoginRequest(
    val usuario: String,
    val password: String
)

data class LoginResponse(
    val id: Long,
    val usuario: String,
    val nombre: String,
    val rol: String
)

data class PostulanteRequest(
    val nombre: String,
    val correo: String,
    val telefono: String,
    val area: String,
    val aniosExperiencia: Int,
    val resumenExperiencia: String,
    val linkedinUrl: String,
    val cvNombre: String? = null,
    val registradaPor: String
)

data class PostulanteDto(
    val id: Long,
    val nombre: String,
    val correo: String,
    val telefono: String,
    val area: String,
    val aniosExperiencia: Int,
    val resumenExperiencia: String,
    val linkedinUrl: String,
    val cvNombre: String? = null,
    val estado: String = "POSTULADA",
    val fechaRegistro: Long,
    val registradaPor: String
)

data class EstadoRequest(
    val estado: String
)

data class PuntajeDto(
    val criterioId: Long,
    val puntaje: Int,
    val justificacion: String
)

data class EvaluacionDto(
    val id: Long? = null,
    val postulanteId: Long,
    val evaluadora: String,
    val evaluadoraClave: String,
    val puntajeTotal: Double,
    val observacionGeneral: String,
    val fecha: Long,
    val puntajes: List<PuntajeDto> = emptyList()
)