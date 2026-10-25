package com.example.wim100.model

data class ResumenCriterios(
    val fortalezas: List<String>,
    val adecuados: List<String>,
    val porMejorar: List<String>
)

object AnalisisEvaluacion {
    fun resumir(criterios: List<Criterio>, notas: Map<Long, Int>): ResumenCriterios = ResumenCriterios(
        fortalezas = criterios.filter { (notas[it.id] ?: 0) >= 4 }.map { it.nombre },
        adecuados = criterios.filter { notas[it.id] == 3 }.map { it.nombre },
        porMejorar = criterios.filter { (notas[it.id] ?: 0) in 1..2 }.map { it.nombre }
    )

    fun etiqueta(nota: Int?): String = when (nota) {
        4, 5 -> "Fortaleza"
        3 -> "Nivel adecuado"
        1, 2 -> "Por mejorar"
        else -> "Sin nota"
    }
}

object ComparacionCandidatas {
    const val MAXIMO = 3

    fun validar(candidatas: List<Candidata>, evaluadas: Set<Long>): String? = when {
        candidatas.size !in 2..MAXIMO -> "Selecciona dos o tres candidatas para comparar."
        candidatas.map { it.id }.distinct().size != candidatas.size -> "Selecciona candidatas diferentes."
        candidatas.any { it.id !in evaluadas } -> "Solo se pueden comparar candidatas evaluadas."
        candidatas.map { it.area }.distinct().size != 1 -> "Las candidatas deben pertenecer a la misma área."
        else -> null
    }

    // Sin nota siempre queda al final, tanto en orden ascendente como descendente.
    fun ordenar(lista: List<ResultadoConsolidado>, puntajes: Map<Long, Double?>, ascendente: Boolean): List<ResultadoConsolidado> =
        lista.sortedWith(Comparator { a, b ->
            val pa = puntajes[a.candidataId]
            val pb = puntajes[b.candidataId]
            val orden = when {
                pa == null && pb == null -> 0
                pa == null -> 1
                pb == null -> -1
                ascendente -> pa.compareTo(pb)
                else -> pb.compareTo(pa)
            }
            if (orden != 0) orden else {
                val nombre = a.nombre.compareTo(b.nombre, ignoreCase = true)
                if (nombre != 0) nombre else a.candidataId.compareTo(b.candidataId)
            }
        })
}
