package com.example.wim100.model

object CalculadoraPuntaje {
    const val PUNTAJE_MINIMO = 1
    const val PUNTAJE_MAXIMO = 5

    fun calcular(criterios: List<Criterio>, puntajes: Map<Long, Int>): Double {
        var pesoTotal = 0
        var obtenido = 0

        // Recorremos cada criterio uno por uno
        for (criterio in criterios) {
            pesoTotal += criterio.peso

            // Buscamos la nota. Si no existe, usamos 0
            val nota = puntajes[criterio.id] ?: 0
            obtenido += nota * criterio.peso
        }

        if (pesoTotal == 0) return 0.0
        return obtenido.toDouble() / (pesoTotal * PUNTAJE_MAXIMO) * 100.0
    }

    fun categoria(puntaje: Double): String = when {
        puntaje >= 85 -> "Sobresaliente"
        puntaje >= 70 -> "Sólida"
        puntaje >= 50 -> "En desarrollo"
        puntaje > 0 -> "Inicial"
        else -> "Sin puntaje"
    }

    fun nivel(puntaje: Int?): String = when (puntaje) {
        1 -> "Insuficiente"
        2 -> "Básico"
        3 -> "Adecuado"
        4 -> "Destacado"
        5 -> "Sobresaliente"
        else -> "Sin asignar"
    }
}