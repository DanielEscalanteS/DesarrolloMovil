package com.example.wim100.navigation

object Rutas {
    const val INICIO = "inicio"
    const val LOGIN = "login"
    const val CANDIDATAS = "candidatas"
    const val CANDIDATA_NUEVA = "candidata/nueva"
    const val CANDIDATA_DETALLE = "candidata/{candidataId}"
    const val CANDIDATA_EDITAR = "candidata/{candidataId}/editar"
    const val EVALUACION = "evaluacion/{candidataId}"
    const val DIRECCION = "direccion"
    const val HISTORIAL = "historial"
    const val DETALLE_EVALUACIONES = "detalle-evaluaciones/{candidataId}"
    const val COMPARAR = "comparar/{ids}"

    fun detalleCandidata(candidataId: Long): String {
        return "candidata/$candidataId"
    }

    fun editarCandidata(candidataId: Long): String {
        return "candidata/$candidataId/editar"
    }

    fun evaluacion(candidataId: Long): String {
        return "evaluacion/$candidataId"
    }

    fun detalleEvaluaciones(candidataId: Long): String {
        return "detalle-evaluaciones/$candidataId"
    }

    fun comparar(ids: List<Long>): String {
        return "comparar/${ids.joinToString(",")}"
    }
}
