
package com.example.wim100.validation

import org.junit.Assert.*
import org.junit.Test

class CandidataValidatorCvTest {

    private val limite = 5L * 1024 * 1024

    @Test
    fun cvSinArchivoEsInvalido() {
        val resultado = CandidataValidator.validarCv(null, null)

        assertFalse(resultado.esValido)
        assertEquals("Falta adjuntar el PDF", resultado.mensaje)
    }

    @Test
    fun cvConNombreVacioEsInvalido() {
        val resultado = CandidataValidator.validarCv("  ", 1024L)

        assertFalse(resultado.esValido)
    }

    @Test
    fun cvConExtensionIncorrectaEsInvalido() {
        val resultado = CandidataValidator.validarCv("curriculum.docx", 1024L)

        assertFalse(resultado.esValido)
        assertEquals("Debe ser PDF", resultado.mensaje)
    }

    @Test
    fun cvSinTamanoEsInvalido() {
        val resultado = CandidataValidator.validarCv("curriculum.pdf", null)

        assertFalse(resultado.esValido)
    }

    @Test
    fun cvVacioEsInvalido() {
        val resultado = CandidataValidator.validarCv("curriculum.pdf", 0L)

        assertFalse(resultado.esValido)
    }

    @Test
    fun cvMayorA5MbEsInvalido() {
        val resultado = CandidataValidator.validarCv(
            "curriculum.pdf",
            limite + 1
        )

        assertFalse(resultado.esValido)
        assertEquals("El PDF supera los 5 MB", resultado.mensaje)
    }

    @Test
    fun cvDe5MbExactosEsValido() {
        val resultado = CandidataValidator.validarCv(
            "curriculum.pdf",
            limite
        )

        assertTrue(resultado.esValido)
    }

    @Test
    fun cvPdfConExtensionMayusculaEsValido() {
        val resultado = CandidataValidator.validarCv(
            "CURRICULUM.PDF",
            1024L
        )

        assertTrue(resultado.esValido)
    }
}
