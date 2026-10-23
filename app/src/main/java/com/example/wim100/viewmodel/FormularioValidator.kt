package com.example.wim100.viewmodel

object FormularioValidator {
    fun validarNombre(nombre: String): String? {
        return when {
            nombre.isBlank() ->
                "El nombre es obligatorio"
            nombre.trim().length < 3 ->
                "El nombre debe tener al menos 3 caracteres"
            else -> null
        }
    }
    fun validarCorreo(correo: String): String? {
        val formatoCorreo =
            Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return when {
            correo.isBlank() ->
                "El correo es obligatorio"
            !formatoCorreo.matches(correo.trim()) ->
                "Ingresa un correo electrónico válido"
            correo.trim().substringAfter("@").lowercase() !in setOf("ejemplo.cl", "example.com") ->
                "Usa un correo ficticio (ej: @ejemplo.cl)"
            else -> null
        }
    }
    fun validarTelefono(telefono: String): String? {
        val numero = telefono.filter { it.isDigit() }
        return when {
            telefono.isBlank() ->
                "El teléfono es obligatorio"
            telefono.any { !it.isDigit() && it !in " +-()" } ->
                "Ingresa un teléfono válido"
            numero.length !in 8..15 ->
                "El teléfono debe tener entre 8 y 15 dígitos"
            else -> null
        }
    }
    fun validarFormulario(
        nombre: String,
        correo: String,
        telefono: String
    ): ErroresFormulario {
        return ErroresFormulario(
            nombre = validarNombre(nombre),
            correo = validarCorreo(correo),
            telefono = validarTelefono(telefono)
        )
    }
}
