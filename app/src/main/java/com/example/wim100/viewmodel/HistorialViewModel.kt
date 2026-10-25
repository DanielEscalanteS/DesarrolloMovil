package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.repository.HistorialRepository
import com.example.wim100.model.RegistroHistorial
import kotlinx.coroutines.flow.*

data class HistorialUiState(
    val registros: List<RegistroHistorial> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

class HistorialViewModel(repository: HistorialRepository) : ViewModel() {
    val uiState = repository.observarTodo()
        .map { HistorialUiState(registros = it, cargando = false) }
        .catch { emit(HistorialUiState(cargando = false, error = "No se pudo cargar el historial")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistorialUiState())
}
