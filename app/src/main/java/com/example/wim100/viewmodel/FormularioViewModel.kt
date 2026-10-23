package com.example.wim100.viewmodel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FormularioViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FormularioUiState())
    val uiState: StateFlow<FormularioUiState> =
        _uiState.asStateFlow()
    fun actualizarNombre(nombre: String) {
        actualizarEstado { it.copy(nombre = nombre) }
    }
    fun actualizarCorreo(correo: String) {
        actualizarEstado { it.copy(correo = correo) }
    }
    fun actualizarTelefono(telefono: String) {
        actualizarEstado { it.copy(telefono = telefono) }
    }
    private fun actualizarEstado(
        cambio: (FormularioUiState) -> FormularioUiState
    ) {
        _uiState.update { estado ->
            val nuevoEstado = cambio(estado)

            if (nuevoEstado.intentoContinuar) {
                validarEstado(nuevoEstado)
            } else {
                nuevoEstado.copy(formularioValido = false)
            }
        }
    }
    fun validarFormulario(): Boolean {
        _uiState.update { estado ->
            validarEstado(
                estado.copy(intentoContinuar = true)
            )
        }

        return _uiState.value.formularioValido
    }
    private fun validarEstado(
        estado: FormularioUiState
    ): FormularioUiState {
        val errores = FormularioValidator.validarFormulario(
            estado.nombre,
            estado.correo,
            estado.telefono
        )
        return estado.copy(
            errores = errores,
            formularioValido = !errores.hayErrores
        )
    }
    fun limpiarFormulario() {
        _uiState.value = FormularioUiState()
    }
}
