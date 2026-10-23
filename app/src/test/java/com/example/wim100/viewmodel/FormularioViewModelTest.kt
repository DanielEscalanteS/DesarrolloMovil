package com.example.wim100.viewmodel
import org.junit.Assert.*
import org.junit.Test

class FormularioViewModelTest {
    @Test
    fun estadoInicialEstaVacio() {
        val viewModel = FormularioViewModel()
        val estado = viewModel.uiState.value
        assertEquals("", estado.nombre)
        assertEquals("", estado.correo)
        assertEquals("", estado.telefono)
        assertFalse(estado.formularioValido)
    }
    @Test
    fun actualizarNombreCambiaEstado() {
        val viewModel = FormularioViewModel()
        viewModel.actualizarNombre("Johaquin")
        assertEquals(
            "Johaquin",
            viewModel.uiState.value.nombre
        )
    }
    @Test
    fun actualizarCorreoCambiaEstado() {
        val viewModel = FormularioViewModel()
        viewModel.actualizarCorreo("usuario@gmail.com")
        assertEquals(
            "usuario@gmail.com",
            viewModel.uiState.value.correo
        )
    }
    @Test
    fun actualizarTelefonoCambiaEstado() {
        val viewModel = FormularioViewModel()
        viewModel.actualizarTelefono("912345678")
        assertEquals(
            "912345678",
            viewModel.uiState.value.telefono
        )
    }
    @Test
    fun formularioVacioEsInvalido() {
        val viewModel = FormularioViewModel()
        val resultado = viewModel.validarFormulario()
        assertFalse(resultado)
        assertTrue(viewModel.uiState.value.errores.hayErrores)
        assertTrue(viewModel.uiState.value.intentoContinuar)
    }
    @Test
    fun formularioCompletoEsValido() {
        val viewModel = FormularioViewModel()
        viewModel.actualizarNombre("Johaquin")
        viewModel.actualizarCorreo("usuario@gmail.com")
        viewModel.actualizarTelefono("912345678")
        val resultado = viewModel.validarFormulario()
        assertTrue(resultado)
        assertTrue(viewModel.uiState.value.formularioValido)
        assertFalse(viewModel.uiState.value.errores.hayErrores)
    }
    @Test
    fun corregirDatosActualizaErrores() {
        val viewModel = FormularioViewModel()

        viewModel.validarFormulario()

        assertTrue(viewModel.uiState.value.errores.hayErrores)

        viewModel.actualizarNombre("Johaquin")
        viewModel.actualizarCorreo("usuario@gmail.com")
        viewModel.actualizarTelefono("912345678")

        assertFalse(viewModel.uiState.value.errores.hayErrores)
        assertTrue(viewModel.uiState.value.formularioValido)
    }
    @Test
    fun modificarFormularioValidoDetectaError() {
        val viewModel = FormularioViewModel()
        viewModel.actualizarNombre("Johaquin")
        viewModel.actualizarCorreo("usuario@gmail.com")
        viewModel.actualizarTelefono("912345678")
        viewModel.validarFormulario()
        viewModel.actualizarCorreo("correo-invalido")
        assertFalse(viewModel.uiState.value.formularioValido)
        assertNotNull(viewModel.uiState.value.errores.correo)
    }
    @Test
    fun limpiarFormularioRestableceEstado() {
        val viewModel = FormularioViewModel()
        viewModel.actualizarNombre("Johaquin")
        viewModel.actualizarCorreo("usuario@gmail.com")
        viewModel.actualizarTelefono("912345678")
        viewModel.validarFormulario()
        viewModel.limpiarFormulario()
        assertEquals(
            FormularioUiState(),
            viewModel.uiState.value
        )
    }
}
