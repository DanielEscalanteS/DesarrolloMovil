package com.example.wim100.model

enum class Rol(val etiqueta: String, val descripcion: String) {
    POSTULANTE(
        "Postulante",
        "Completa y envía sus antecedentes para evaluación."
    ),
    EVALUADORA(
        "Evaluadora",
        "Registra candidaturas y las evalúa con la matriz WIM."
    ),
    DIRECCION(
        "Dirección",
        "Consulta resultados consolidados y resuelve candidaturas evaluadas."
    )
}

data class Sesion(
    val nombre: String,
    val rol: Rol
)