package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.DatosEjemploRepository
import com.example.wim100.model.Candidata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DatosEjemploUiState(
    val registrosAnteriores: List<Candidata> = emptyList(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null
)

class DatosEjemploViewModel(
    candidataRepository: CandidataRepository,
    private val repository: DatosEjemploRepository
) : ViewModel() {
    private val operacion = MutableStateFlow(DatosEjemploUiState())
    val uiState = combine(candidataRepository.observarTodas(), operacion) { candidatas, estado ->
        estado.copy(cargando = false, registrosAnteriores = candidatas.filter { it.registradaPor != DatosEjemploRepository.RESPONSABLE })
    }.catch { emit(DatosEjemploUiState(cargando = false, error = "No se pudieron cargar los registros anteriores.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DatosEjemploUiState())

    fun cargar(reemplazarId: Long?) {
        if (operacion.value.guardando) return
        operacion.update { it.copy(guardando = true, mensaje = null, error = null) }
        viewModelScope.launch {
            try {
                val mensaje = repository.cargar(reemplazarId)
                operacion.update { it.copy(guardando = false, mensaje = mensaje) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                operacion.update { it.copy(guardando = false, error = e.message ?: "No se pudieron preparar los ejemplos.") }
            }
        }
    }
}
