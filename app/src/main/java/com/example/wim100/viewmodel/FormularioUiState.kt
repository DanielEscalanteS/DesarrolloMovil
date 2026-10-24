package com.example.wim100.viewmodel

data class FormularioUiState(
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",
    val area: String = "",
    val aniosExperiencia: String = "",
    val resumenExperiencia: String = "",
    val linkedinUrl: String = "",
    val cvUri: String? = null,
    val cvNombre: String? = null,
    val cvTamano: Long? = null,
    val guardando: Boolean = false,
    val idGuardado: Long? = null,
    val errorGuardado: String? = null,
    val errores: ErroresFormulario = ErroresFormulario(),
    val intentoContinuar: Boolean = false,
    val formularioValido: Boolean = false
)

data class ErroresFormulario(
    val nombre: String? = null,
    val correo: String? = null,
    val telefono: String? = null,
    val area: String? = null,
    val aniosExperiencia: String? = null,
    val resumenExperiencia: String? = null,
    val linkedinUrl: String? = null,
    val cv: String? = null
) {
    val hayErrores: Boolean
        get() = nombre != null ||
                correo != null ||
                telefono != null ||
                area != null ||
                aniosExperiencia != null ||
                resumenExperiencia != null ||
                linkedinUrl != null ||
                cv != null
}
