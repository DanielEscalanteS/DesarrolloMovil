package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.local.CandidataConEvaluaciones
import com.example.wim100.data.local.EvaluacionConPuntajes
import com.example.wim100.data.repository.EvaluacionRepository
import com.example.wim100.model.Criterio
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

data class DetalleEvaluacionesUiState(
    val ficha: CandidataConEvaluaciones? = null,
    val criterios: List<Criterio> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleEvaluacionesViewModel(private val repository: EvaluacionRepository) : ViewModel() {
    private val candidataId = MutableStateFlow<Long?>(null)

    val uiState = candidataId.flatMapLatest { id ->
        if (id == null) flowOf(DetalleEvaluacionesUiState())
        else combine(repository.observarFicha(id), repository.observarCriterios()) { ficha, criterios ->
            DetalleEvaluacionesUiState(
                ficha = ficha?.copy(evaluaciones = ficha.evaluaciones.sortedWith(
                    compareByDescending<EvaluacionConPuntajes> { it.evaluacion.fecha }
                        .thenByDescending { it.evaluacion.id }
                )),
                criterios = criterios.sortedBy { it.orden },
                cargando = false,
                error = if (ficha == null) "La candidatura ya no está disponible." else null
            )
        }.onStart { emit(DetalleEvaluacionesUiState()) }
            .catch { emit(DetalleEvaluacionesUiState(cargando = false, error = "No se pudieron cargar las evaluaciones.")) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleEvaluacionesUiState())

    fun cargar(id: Long) { candidataId.value = id }
}
