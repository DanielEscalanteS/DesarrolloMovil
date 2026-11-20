package com.example.wim100.data.remote

data class PostulanteDto(
    val id: Long,
    val nombre: String,
    val correo: String,
    val telefono: String,
    val area: String,
    val aniosExperiencia: Int,
    val resumenExperiencia: String,
    val linkedinUrl: String,
    val cvNombre: String?,
    val estado: String,
    val fechaRegistro: Long,
    val registradaPor: String
)

data class PostulanteRequest(
    val nombre: String,
    val correo: String,
    val telefono: String,
    val area: String,
    val aniosExperiencia: Int,
    val resumenExperiencia: String,
    val linkedinUrl: String,
    val cvNombre: String?,
    val registradaPor: String
)

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

data class EstadoRequest(
    val estado: String
)
