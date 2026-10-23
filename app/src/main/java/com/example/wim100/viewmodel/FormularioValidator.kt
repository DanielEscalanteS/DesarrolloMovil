package com.example.wim100.viewmodel

import com.example.wim100.validation.CandidataValidator

/** Adaptador para mantener el contrato del formulario con una sola fuente de reglas. */
object FormularioValidator {
    fun validarNombre(nombre: String): String? = CandidataValidator.validarNombre(nombre).mensaje
    fun validarCorreo(correo: String): String? = CandidataValidator.validarCorreo(correo).mensaje
    fun validarTelefono(telefono: String): String? = CandidataValidator.validarTelefono(telefono).mensaje

    fun validarFormulario(nombre: String, correo: String, telefono: String): ErroresFormulario =
        ErroresFormulario(validarNombre(nombre), validarCorreo(correo), validarTelefono(telefono))
}
