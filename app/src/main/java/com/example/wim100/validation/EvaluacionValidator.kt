package com.example.wim100.validation

object EvaluacionValidator {
    const val OBSERVACION_GENERAL_MAX = 400
    const val OBSERVACION_CRITERIO_MAX = 300

    fun validarPuntaje(puntaje: Int?): ValidationResult {
        if (puntaje == null) return ValidationResult.error("Asigna un puntaje")
        if (puntaje < 1 || puntaje > 5) return ValidationResult.error("El puntaje es de 1 a 5")
        return ValidationResult.Valido
    }

    fun validarObservacionCriterio(puntaje: Int?, observacion: String): ValidationResult {
        val texto = observacion.trim()
        if (texto.length > OBSERVACION_CRITERIO_MAX) return ValidationResult.error("Texto muy largo")

        // Si la nota es 1 o 2, es obligación justificar
        if (puntaje != null && puntaje <= 2 && texto.length < 10) {
            return ValidationResult.error("Justifica esta nota baja (mín. 10 caracteres)")
        }
        return ValidationResult.Valido
    }

    fun validarObservacionGeneral(texto: String): ValidationResult {
        if (texto.trim().length > OBSERVACION_GENERAL_MAX) return ValidationResult.error("Máximo $OBSERVACION_GENERAL_MAX caracteres")
        if (texto.trim().length < 20) return ValidationResult.error("Mínimo 20 caracteres")
        return ValidationResult.Valido
    }
}