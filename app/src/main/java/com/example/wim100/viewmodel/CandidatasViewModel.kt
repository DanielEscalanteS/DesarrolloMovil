package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.model.Candidata
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CandidatasUiState(
    val candidatas: List<Candidata> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

class CandidatasViewModel(repository: CandidataRepository) : ViewModel() {
    val uiState = repository.observarTodas()
        .map { CandidatasUiState(candidatas = it, cargando = false) }
        .catch { emit(CandidatasUiState(cargando = false, error = "No se pudieron cargar las postulaciones")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CandidatasUiState())
}
