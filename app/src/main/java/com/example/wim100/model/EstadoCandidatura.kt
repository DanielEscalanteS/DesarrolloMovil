package com.example.wim100.model
enum class EstadoCandidatura(val etiqueta: String) {
    POSTULADA("Postulada"),
    EN_EVALUACION("En evaluación"),
    EVALUADA("Evaluada"),
    SELECCIONADA("Seleccionada"),
    NO_SELECCIONADA("No seleccionada");

    // Una candidatura resuelta por Dirección ya no admite nuevas evaluaciones.
    val estaResuelta: Boolean
        get() = this == SELECCIONADA || this == NO_SELECCIONADA
}