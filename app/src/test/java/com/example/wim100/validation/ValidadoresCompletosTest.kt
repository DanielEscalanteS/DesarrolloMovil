package com.example.wim100.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadoresCompletosTest {

    @Test
    fun evaluacionValidatorValidaPuntajesYObservaciones() {
        assertFalse(EvaluacionValidator.validarPuntaje(null).esValido)
        assertFalse(EvaluacionValidator.validarPuntaje(0).esValido)
        assertFalse(EvaluacionValidator.validarPuntaje(6).esValido)
        assertTrue(EvaluacionValidator.validarPuntaje(4).esValido)

        assertFalse(EvaluacionValidator.validarObservacionCriterio(2, "Corta").esValido)
        assertTrue(EvaluacionValidator.validarObservacionCriterio(1, "Justificación suficiente").esValido)
        assertTrue(EvaluacionValidator.validarObservacionCriterio(4, "").esValido)
        assertFalse(EvaluacionValidator.validarObservacionCriterio(4, "a".repeat(301)).esValido)

        assertFalse(EvaluacionValidator.validarObservacionGeneral("Muy corta").esValido)
        assertFalse(EvaluacionValidator.validarObservacionGeneral("a".repeat(401)).esValido)
        assertTrue(
            EvaluacionValidator.validarObservacionGeneral(
                "Observación general completa de más de veinte caracteres."
            ).esValido
        )
    }

    @Test
    fun candidataValidatorValidaCamposDePostulacion() {
        assertFalse(CandidataValidator.validarNombre("").esValido)
        assertFalse(CandidataValidator.validarNombre("Ab").esValido)
        assertTrue(CandidataValidator.validarNombre("Ana").esValido)

        assertFalse(CandidataValidator.validarCorreo("").esValido)
        assertFalse(CandidataValidator.validarCorreo("sin-arroba").esValido)
        assertFalse(CandidataValidator.validarCorreo("ana@gmail.com").esValido)
        assertTrue(CandidataValidator.validarCorreo("ana@ejemplo.cl").esValido)

        assertFalse(CandidataValidator.validarTelefono("").esValido)
        assertFalse(CandidataValidator.validarTelefono("123").esValido)
        assertTrue(CandidataValidator.validarTelefono("+56 9 1234 5678").esValido)
        assertEquals("912345678", CandidataValidator.formatearTelefono(" 912345678 "))

        assertFalse(CandidataValidator.validarArea(null).esValido)
        assertTrue(CandidataValidator.validarArea("Blue Team").esValido)

        assertFalse(CandidataValidator.validarAnios("abc").esValido)
        assertFalse(CandidataValidator.validarAnios("-1").esValido)
        assertTrue(CandidataValidator.validarAnios("3").esValido)

        assertFalse(CandidataValidator.validarResumen("Corto").esValido)
        assertTrue(CandidataValidator.validarResumen("a".repeat(30)).esValido)

        assertFalse(CandidataValidator.validarLinkedin("https://google.com").esValido)
        assertTrue(CandidataValidator.validarLinkedin("https://www.linkedin.com/in/ana-demo").esValido)
    }
}