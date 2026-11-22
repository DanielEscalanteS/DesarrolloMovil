package com.example.wim100.model

import com.example.wim100.viewmodel.candidataRemotaTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelosTest {

    @Test
    fun criteriosDemostracionSumanCienPorCiento() {
        val criterios = CriteriosDemostracion.lista
        assertEquals(100, criterios.sumOf { it.peso })
    }

    @Test
    fun calculadoraPuntajeCategoriaYNivelCubrenTodosLosRangos() {
        val criterios = listOf(
            Criterio(1L, "A", "", 60, 1),
            Criterio(2L, "B", "", 40, 2)
        )
        assertEquals(0.0, CalculadoraPuntaje.calcular(emptyList(), emptyMap()), 0.01)
        assertEquals(84.0, CalculadoraPuntaje.calcular(criterios, mapOf(1L to 5, 2L to 3)), 0.01)

        assertEquals("Sobresaliente", CalculadoraPuntaje.categoria(90.0))
        assertEquals("Sólida", CalculadoraPuntaje.categoria(75.0))
        assertEquals("En desarrollo", CalculadoraPuntaje.categoria(55.0))
        assertEquals("Inicial", CalculadoraPuntaje.categoria(20.0))
        assertEquals("Sin puntaje", CalculadoraPuntaje.categoria(0.0))

        assertEquals("Insuficiente", CalculadoraPuntaje.nivel(1))
        assertEquals("Básico", CalculadoraPuntaje.nivel(2))
        assertEquals("Adecuado", CalculadoraPuntaje.nivel(3))
        assertEquals("Destacado", CalculadoraPuntaje.nivel(4))
        assertEquals("Sobresaliente", CalculadoraPuntaje.nivel(5))
        assertEquals("Sin asignar", CalculadoraPuntaje.nivel(null))
    }

    @Test
    fun analisisEtiquetasYResultadoConsolidadoCubrenEstados() {
        assertEquals("Fortaleza", AnalisisEvaluacion.etiqueta(5))
        assertEquals("Nivel adecuado", AnalisisEvaluacion.etiqueta(3))
        assertEquals("Por mejorar", AnalisisEvaluacion.etiqueta(1))
        assertEquals("Sin nota", AnalisisEvaluacion.etiqueta(null))

        val resuelta = ResultadoConsolidado(1, "A", "Area", EstadoCandidatura.SELECCIONADA, 1, 90.0)
        val sinEval = ResultadoConsolidado(2, "B", "Area", EstadoCandidatura.POSTULADA, 0, null)
        val noEvaluada = ResultadoConsolidado(3, "C", "Area", EstadoCandidatura.EN_EVALUACION, 1, 70.0)
        val listaParaResolver = ResultadoConsolidado(4, "D", "Area", EstadoCandidatura.EVALUADA, 1, 85.0)

        assertNotNull(resuelta.motivoBloqueoResolucion)
        assertNotNull(sinEval.motivoBloqueoResolucion)
        assertNotNull(noEvaluada.motivoBloqueoResolucion)
        assertNull(listaParaResolver.motivoBloqueoResolucion)
        assertTrue(EstadoCandidatura.NO_SELECCIONADA.estaResuelta)
        assertEquals("ana pérez", Evaluacion.normalizarClave("  ANA   PÉREZ "))
    }

    @Test
    fun comparacionAceptaTresCandidatasEvaluadasDeLaMismaArea() {
        val candidatas = listOf(
            candidataRemotaTest(1L),
            candidataRemotaTest(2L),
            candidataRemotaTest(3L)
        )
        assertNull(ComparacionCandidatas.validar(candidatas, setOf(1L, 2L, 3L)))
    }
}