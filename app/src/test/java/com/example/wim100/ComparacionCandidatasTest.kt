package com.example.wim100

import com.example.wim100.model.*
import org.junit.Assert.*
import org.junit.Test

class ComparacionCandidatasTest {
    private fun candidata(id: Long, area: String = "Blue Team") = Candidata(
        id = id, nombre = "Candidata $id", correo = "$id@example.com", telefono = "912345678", area = area,
        aniosExperiencia = 2, resumenExperiencia = "Perfil ficticio de prueba", linkedinUrl = "https://www.linkedin.com/in/prueba",
        cvUri = null, cvNombre = null, registradaPor = "Demo")

    @Test fun soloComparaDosOTresCandidatasDistintasEvaluadasDeLaMismaArea() {
        val evaluadas = setOf(1L, 2L, 3L, 4L)
        assertNull(ComparacionCandidatas.validar(listOf(candidata(1), candidata(2)), evaluadas))
        assertNotNull(ComparacionCandidatas.validar(listOf(candidata(1)), evaluadas))
        assertNotNull(ComparacionCandidatas.validar(listOf(candidata(1), candidata(1)), evaluadas))
        assertNotNull(ComparacionCandidatas.validar(listOf(candidata(1), candidata(2, "Red Team")), evaluadas))
        assertNotNull(ComparacionCandidatas.validar(listOf(candidata(1), candidata(5)), evaluadas))
        assertNotNull(ComparacionCandidatas.validar((1L..4L).map { candidata(it) }, evaluadas))
    }

    @Test fun ordenarPorCriterioNoModificaLosTotalesYDejaPendientesAlFinal() {
        val lista = listOf(
            ResultadoConsolidado(1, "Ana", "Blue Team", EstadoCandidatura.EVALUADA, 1, 90.0),
            ResultadoConsolidado(2, "Beatriz", "Blue Team", EstadoCandidatura.EVALUADA, 1, 80.0),
            ResultadoConsolidado(3, "Camila", "Blue Team", EstadoCandidatura.POSTULADA, 0, null))
        val porLiderazgo = mapOf(1L to 2.0, 2L to 5.0, 3L to null)
        assertEquals(listOf(2L, 1L, 3L), ComparacionCandidatas.ordenar(lista, porLiderazgo, false).map { it.candidataId })
        assertEquals(listOf(1L, 2L, 3L), ComparacionCandidatas.ordenar(lista, porLiderazgo, true).map { it.candidataId })
        assertEquals(90.0, lista.first().promedio!!, 0.001)
        assertEquals(listOf(1L, 2L, 3L), ComparacionCandidatas.ordenar(lista, lista.associate { it.candidataId to it.promedio }, false).map { it.candidataId })
    }

    @Test fun notasAusentesNoSeEtiquetanComoDebilidades() {
        val criterios = (1L..5L).map { Criterio(id = it, nombre = "Criterio $it", descripcion = "Prueba", peso = 20, orden = it.toInt()) }
        val resumen = AnalisisEvaluacion.resumir(criterios, mapOf(1L to 5, 2L to 4, 3L to 3, 4L to 2))
        assertEquals(listOf("Criterio 1", "Criterio 2"), resumen.fortalezas)
        assertEquals(listOf("Criterio 3"), resumen.adecuados)
        assertEquals(listOf("Criterio 4"), resumen.porMejorar)
    }
}
