package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.repository.EvaluacionRepository
import com.example.wim100.model.CalculadoraPuntaje
import com.example.wim100.model.Criterio
import com.example.wim100.validation.EvaluacionValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EvaluacionUiState(
    val criterios: List<Criterio> = emptyList(),
    val puntajes: Map<Long, Int> = emptyMap(),
    val observaciones: Map<Long, String> = emptyMap(),
    val observacionGeneral: String = "",
    val errorGeneral: String? = null,
    val guardando: Boolean = false,
    val finalizadoConExito: Boolean = false
) {
    // Para calcular la nota en tiempo real mientras el usuario teclea
    val puntajeTotal: Double get() = CalculadoraPuntaje.calcular(criterios, puntajes)
}

class EvaluacionViewModel(private val repository: EvaluacionRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(EvaluacionUiState())
    val uiState: StateFlow<EvaluacionUiState> = _uiState.asStateFlow()

    // Carga desde la base de datos al abrir la pantalla
    fun cargarCriterios() {
        viewModelScope.launch {
            // 1. Inyectamos los criterios oficiales de WIM100 si la base de datos está vacía
            val criteriosOficiales = listOf(
                Criterio(nombre = "Trayectoria", descripcion = "Años y relevancia de experiencia.", peso = 25, orden = 1),
                Criterio(nombre = "Conocimientos", descripcion = "Certificaciones y educación.", peso = 25, orden = 2),
                Criterio(nombre = "Liderazgo", descripcion = "Habilidades de gestión y equipo.", peso = 20, orden = 3),
                Criterio(nombre = "Visión", descripcion = "Perspectiva estratégica en ciberseguridad.", peso = 15, orden = 4),
                Criterio(nombre = "Comunicación", descripcion = "Claridad en la entrega de ideas.", peso = 15, orden = 5)
            )
            repository.asegurarCriterios(criteriosOficiales)

            // 2. Se leen y se muestran en pantalla
            val lista = repository.obtenerCriterios()
            _uiState.update { it.copy(criterios = lista) }
        }
    }

    // Actualiza la nota de un criterio en específico
    fun actualizarPuntaje(criterioId: Long, nota: Int) {
        _uiState.update { estado ->
            val nuevosPuntajes = estado.puntajes + (criterioId to nota)
            estado.copy(puntajes = nuevosPuntajes)
        }
    }

    fun actualizarObservacion(criterioId: Long, texto: String) {
        _uiState.update { it.copy(observaciones = it.observaciones + (criterioId to texto)) }
    }

    fun actualizarObservacionGeneral(texto: String) {
        _uiState.update { it.copy(observacionGeneral = texto) }
    }

    // Valida y guarda en la base de datos (Room)
    fun guardarEvaluacion(candidataId: Long, nombreEvaluadora: String) {
        val estado = _uiState.value

        // 1. Verificar si la observación general no está vacía
        val validacionGeneral = EvaluacionValidator.validarObservacionGeneral(estado.observacionGeneral)
        if (!validacionGeneral.esValido) {
            _uiState.update { it.copy(errorGeneral = validacionGeneral.mensaje) }
            return
        }

        _uiState.update { it.copy(guardando = true, errorGeneral = null) }
        viewModelScope.launch {
            try {
                repository.registrar(
                    candidataId = candidataId,
                    evaluadora = nombreEvaluadora,
                    puntajes = estado.puntajes,
                    observaciones = estado.observaciones,
                    observacionGeneral = estado.observacionGeneral
                )
                _uiState.update { it.copy(guardando = false, finalizadoConExito = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(guardando = false, errorGeneral = e.message) }
            }
        }
    }
}