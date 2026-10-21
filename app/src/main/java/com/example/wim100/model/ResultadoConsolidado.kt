package com.example.wim100.model

data class ResultadoConsolidado(
    val candidataId: Long,
    val nombre: String,
    val area: String,
    val estado: EstadoCandidatura,
    val totalEvaluaciones: Int,
    val promedio: Double?
)