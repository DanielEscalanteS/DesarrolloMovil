package com.example.wim100.viewmodel
import org.junit.Assert.*
import org.junit.Test

class FormularioValidatorTest {
    @Test
    fun nombreVacioMuestraError() {
        val resultado = FormularioValidator.validarNombre("")
        assertEquals("El nombre es obligatorio", resultado)
    }
    @Test
    fun nombreMuyCortoMuestraError() {
        val resultado = FormularioValidator.validarNombre("Jo")
        assertEquals(
            "El nombre debe tener al menos 3 caracteres",
            resultado
        )
    }
    @Test
    fun nombreValidoNoTieneErrores() {
        val resultado = FormularioValidator.validarNombre("Johaquin")
        assertNull(resultado)
    }
    @Test
    fun correoInvalidoMuestraError() {
        val resultado = FormularioValidator.validarCorreo("correoincorrecto")
        assertEquals(
            "Ingresa un correo electrónico válido",
            resultado
        )
    }
    @Test
    fun correoValidoNoTieneErrores() {
        val resultado = FormularioValidator.validarCorreo(
            "usuario@example.com"
        )
        assertNull(resultado)
    }
    @Test
    fun telefonoCortoMuestraError() {
        val resultado = FormularioValidator.validarTelefono("1234")
        assertEquals(
            "El teléfono debe tener entre 8 y 15 dígitos",
            resultado
        )
    }
    @Test
    fun telefonoValidoNoTieneErrores() {
        val resultado = FormularioValidator.validarTelefono(
            "+56912345678"
        )
        assertNull(resultado)
    }
    @Test
    fun formularioCompletoEsValido() {
        val errores = FormularioValidator.validarFormulario(
            nombre = "Johaquin",
            correo = "usuario@example.com",
            telefono = "912345678"
        )
        assertFalse(errores.hayErrores)
    }
    @Test
    fun formularioVacioTieneErrores() {
        val errores = FormularioValidator.validarFormulario(
            nombre = "",
            correo = "",
            telefono = ""
        )
        assertTrue(errores.hayErrores)
    }
    @Test
    fun correoRealNoSeAceptaEnElMvpAcademico() {
        assertNotNull(FormularioValidator.validarCorreo("usuario@gmail.com"))
    }

    @Test
    fun correoFicticioAceptaEspaciosExternosYDominioEnMayusculas() {
        assertNull(FormularioValidator.validarCorreo(" usuario@EXAMPLE.COM "))
    }

    @Test
    fun telefonoConLetrasNoSeAceptaAunqueTengaSuficientesDigitos() {
        assertNotNull(FormularioValidator.validarTelefono("abc912345678"))
    }
}