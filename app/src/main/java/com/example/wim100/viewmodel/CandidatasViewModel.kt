package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.EvaluacionRepository
import com.example.wim100.model.Candidata
import com.example.wim100.model.Evaluacion
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CandidatasUiState(
    val candidatas: List<Candidata> = emptyList(),
    val evaluaciones: List<Evaluacion> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
) {
    fun yaEvaluo(candidataId: Long, nombre: String): Boolean = evaluaciones.any {
        it.candidataId == candidataId && it.evaluadoraClave == Evaluacion.normalizarClave(nombre)
    }

    fun totalEvaluaciones(candidataId: Long): Int = evaluaciones.count { it.candidataId == candidataId }
}

class CandidatasViewModel(repository: CandidataRepository, evaluacionRepository: EvaluacionRepository) : ViewModel() {
    val uiState = combine(repository.observarTodas(), evaluacionRepository.observarTodas()) { candidatas, evaluaciones ->
        CandidatasUiState(candidatas = candidatas, evaluaciones = evaluaciones, cargando = false)
    }
        .catch { emit(CandidatasUiState(cargando = false, error = "No se pudieron cargar las postulaciones")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CandidatasUiState())
}
