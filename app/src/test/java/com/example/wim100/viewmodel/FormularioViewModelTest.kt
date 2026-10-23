package com.example.wim100.viewmodel
import org.junit.Assert.*
import org.junit.Test

class FormularioViewModelTest {
    @Test
    fun estadoInicialEstaVacio() {
        val viewModel = crearFormularioViewModel()
        val estado = viewModel.uiState.value
        assertEquals("", estado.nombre)
        assertEquals("", estado.correo)
        assertEquals("", estado.telefono)
        assertFalse(estado.formularioValido)
    }
    @Test
    fun actualizarNombreCambiaEstado() {
        val viewModel = crearFormularioViewModel()
        viewModel.actualizarNombre("Johaquin")
        assertEquals(
            "Johaquin",
            viewModel.uiState.value.nombre
        )
    }
    @Test
    fun actualizarCorreoCambiaEstado() {
        val viewModel = crearFormularioViewModel()
        viewModel.actualizarCorreo("usuario@example.com")
        assertEquals(
            "usuario@example.com",
            viewModel.uiState.value.correo
        )
    }
    @Test
    fun actualizarTelefonoCambiaEstado() {
        val viewModel = crearFormularioViewModel()
        viewModel.actualizarTelefono("912345678")
        assertEquals(
            "912345678",
            viewModel.uiState.value.telefono
        )
    }
    @Test
    fun formularioVacioEsInvalido() {
        val viewModel = crearFormularioViewModel()
        val resultado = viewModel.validarFormulario()
        assertFalse(resultado)
        assertTrue(viewModel.uiState.value.errores.hayErrores)
        assertTrue(viewModel.uiState.value.intentoContinuar)
    }
    @Test
    fun formularioCompletoEsValido() {
        val viewModel = crearFormularioViewModel()
        viewModel.actualizarNombre("Johaquin")
        viewModel.actualizarCorreo("usuario@example.com")
        viewModel.actualizarTelefono("912345678")
        val resultado = viewModel.validarFormulario()
        assertTrue(resultado)
        assertTrue(viewModel.uiState.value.formularioValido)
        assertFalse(viewModel.uiState.value.errores.hayErrores)
    }
    @Test
    fun corregirDatosActualizaErrores() {
        val viewModel = crearFormularioViewModel()

        viewModel.validarFormulario()

        assertTrue(viewModel.uiState.value.errores.hayErrores)

        viewModel.actualizarNombre("Johaquin")
        viewModel.actualizarCorreo("usuario@example.com")
        viewModel.actualizarTelefono("912345678")

        assertFalse(viewModel.uiState.value.errores.hayErrores)
        assertTrue(viewModel.uiState.value.formularioValido)
    }
    @Test
    fun modificarFormularioValidoDetectaError() {
        val viewModel = crearFormularioViewModel()
        viewModel.actualizarNombre("Johaquin")
        viewModel.actualizarCorreo("usuario@example.com")
        viewModel.actualizarTelefono("912345678")
        viewModel.validarFormulario()
        viewModel.actualizarCorreo("correo-invalido")
        assertFalse(viewModel.uiState.value.formularioValido)
        assertNotNull(viewModel.uiState.value.errores.correo)
    }
    @Test
    fun limpiarFormularioRestableceEstado() {
        val viewModel = crearFormularioViewModel()
        viewModel.actualizarNombre("Johaquin")
        viewModel.actualizarCorreo("usuario@example.com")
        viewModel.actualizarTelefono("912345678")
        viewModel.validarFormulario()
        viewModel.limpiarFormulario()
        assertEquals(
            FormularioUiState(),
            viewModel.uiState.value
        )
    }
    @Test
    fun datosPersonalesValidosNoBastanParaEnviarPostulacion() {
        val vm = crearFormularioViewModel()
        vm.actualizarNombre("Persona Ficticia")
        vm.actualizarCorreo("persona@example.com")
        vm.actualizarTelefono("912345678")
        assertTrue(vm.validarFormulario())
        assertFalse(vm.validarPostulacion())
        assertNotNull(vm.uiState.value.errores.cv)
        assertNotNull(vm.uiState.value.errores.area)
        assertNull(vm.uiState.value.idGuardado)
    }

    @Test
    fun postulacionCompletaSeValidaYCorregirCvActualizaError() {
        val vm = crearFormularioViewModel()
        vm.actualizarNombre("Persona Ficticia")
        vm.actualizarCorreo("persona@example.com")
        vm.actualizarTelefono("912345678")
        vm.actualizarArea("Seguridad defensiva (Blue Team)")
        vm.actualizarAnios("2")
        vm.actualizarResumen("Experiencia ficticia suficiente para probar el formulario académico.")
        vm.actualizarLinkedin("https://www.linkedin.com/in/persona-ficticia")
        vm.actualizarCv("file:///demo.pdf", "demo.pdf", 6L * 1024 * 1024)
        assertFalse(vm.validarPostulacion())
        assertNotNull(vm.uiState.value.errores.cv)
        vm.actualizarCv("file:///demo.pdf", "demo.pdf", 1024L)
        assertTrue(vm.uiState.value.formularioValido)
        assertNull(vm.uiState.value.errores.cv)
    }
}
