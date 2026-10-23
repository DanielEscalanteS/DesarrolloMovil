package com.example.wim100.viewmodel

data class FormularioUiState(
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",
    val errores: ErroresFormulario = ErroresFormulario(),
    val intentoContinuar: Boolean = false,
    val formularioValido: Boolean = false
)

data class ErroresFormulario(
    val nombre: String? = null,
    val correo: String? = null,
    val telefono: String? = null
) {
    val hayErrores: Boolean
        get() = nombre != null ||
                correo != null ||
                telefono != null
}
