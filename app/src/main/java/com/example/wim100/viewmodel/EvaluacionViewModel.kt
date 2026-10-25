package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.repository.EvaluacionRepository
import com.example.wim100.model.CalculadoraPuntaje
import com.example.wim100.model.Criterio
import com.example.wim100.model.CriteriosDemostracion
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
            try {
            // 1. Inyectamos los criterios oficiales de WIM100 si la base de datos está vacía
            val criteriosOficiales = CriteriosDemostracion.lista
            repository.asegurarCriterios(criteriosOficiales)

            // 2. Se leen y se muestran en pantalla
            val lista = repository.obtenerCriterios()
            _uiState.update { it.copy(criterios = lista) }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(errorGeneral = "No se pudieron cargar los criterios") } }
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
        if (_uiState.value.guardando || _uiState.value.finalizadoConExito) return
        val estado = _uiState.value
        if (estado.criterios.isEmpty()) {
            _uiState.update { it.copy(errorGeneral = "Espera a que carguen los criterios") }
            return
        }
        for (criterio in estado.criterios) {
            val nota = EvaluacionValidator.validarPuntaje(estado.puntajes[criterio.id])
            val observacion = EvaluacionValidator.validarObservacionCriterio(
                estado.puntajes[criterio.id], estado.observaciones[criterio.id].orEmpty())
            if (!nota.esValido || !observacion.esValido) {
                _uiState.update { it.copy(errorGeneral = "${criterio.nombre}: ${nota.mensaje ?: observacion.mensaje}") }
                return
            }
        }

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
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) {
                _uiState.update { it.copy(guardando = false, errorGeneral = e.message) }
            }
        }
    }
}
