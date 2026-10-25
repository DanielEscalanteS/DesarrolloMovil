package com.example.wim100.model

// La matriz original, compartida por el formulario y los ejemplos.
object CriteriosDemostracion {
    val lista = listOf(
        Criterio(nombre = "Trayectoria", descripcion = "Años y relevancia de experiencia.", peso = 25, orden = 1),
        Criterio(nombre = "Conocimientos", descripcion = "Certificaciones y educación.", peso = 25, orden = 2),
        Criterio(nombre = "Liderazgo", descripcion = "Habilidades de gestión y equipo.", peso = 20, orden = 3),
        Criterio(nombre = "Visión", descripcion = "Perspectiva estratégica en ciberseguridad.", peso = 15, orden = 4),
        Criterio(nombre = "Comunicación", descripcion = "Claridad en la entrega de ideas.", peso = 15, orden = 5)
    )
}
