package com.example.wim100.model

data class ResultadoConsolidado(
    val candidataId: Long,
    val nombre: String,
    val area: String,
    val estado: EstadoCandidatura,
    val totalEvaluaciones: Int,
    val promedio: Double?
) {
    val motivoBloqueoResolucion: String?
        get() = when {
            estado.estaResuelta -> "Resolución registrada: ${estado.etiqueta}."
            totalEvaluaciones == 0 -> "Esta candidata todavía no tiene evaluaciones."
            totalEvaluaciones < Evaluacion.MINIMO_PARA_CONSOLIDAR ->
                "Faltan ${Evaluacion.MINIMO_PARA_CONSOLIDAR - totalEvaluaciones} evaluaciones para resolver."
            estado != EstadoCandidatura.EVALUADA -> "La candidatura aún no está en estado Evaluada."
            else -> null
        }
}
