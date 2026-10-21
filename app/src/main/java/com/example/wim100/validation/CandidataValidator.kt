package com.example.wim100.validation

object CandidataValidator {

    fun validarNombre(valor: String): ValidationResult {
        val texto = valor.trim()
        if (texto.isEmpty()) return ValidationResult.error("El nombre es obligatorio")
        if (texto.length < 3) return ValidationResult.error("Mínimo 3 letras")
        return ValidationResult.Valido
    }

    fun validarCorreo(valor: String): ValidationResult {
        val texto = valor.trim()
        if (texto.isEmpty()) return ValidationResult.error("El correo es obligatorio")
        if (!texto.contains("@")) return ValidationResult.error("Falta el @")
        if (!texto.endsWith("@ejemplo.cl") && !texto.endsWith("@example.com")) {
            return ValidationResult.error("Usa un correo ficticio (ej: @ejemplo.cl)")
        }
        return ValidationResult.Valido
    }

    fun validarTelefono(valor: String): ValidationResult {
        if (valor.trim().isEmpty()) return ValidationResult.error("El teléfono es obligatorio")
        return ValidationResult.Valido
    }

    fun validarArea(valor: String?): ValidationResult {
        if (valor == null || valor.trim().isEmpty()) return ValidationResult.error("Selecciona un área")
        return ValidationResult.Valido
    }

    fun validarAnios(valor: String): ValidationResult {
        val numero = valor.trim().toIntOrNull()
        if (numero == null) return ValidationResult.error("Ingresa un número válido")
        if (numero < 0) return ValidationResult.error("No puede ser negativo")
        return ValidationResult.Valido
    }

    fun validarResumen(valor: String): ValidationResult {
        if (valor.trim().length < 30) return ValidationResult.error("Mínimo 30 caracteres")
        return ValidationResult.Valido
    }

    fun validarLinkedin(valor: String): ValidationResult {
        if (!valor.contains("linkedin.com/in/")) return ValidationResult.error("Ingresa un enlace de LinkedIn válido")
        return ValidationResult.Valido
    }

    fun validarCv(nombreArchivo: String?, tamanoBytes: Long?): ValidationResult {
        if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) return ValidationResult.error("Falta adjuntar el PDF")
        if (!nombreArchivo.endsWith(".pdf", ignoreCase = true)) return ValidationResult.error("Debe ser PDF")
        return ValidationResult.Valido
    }

    fun formatearTelefono(valor: String): String = valor.trim()
}