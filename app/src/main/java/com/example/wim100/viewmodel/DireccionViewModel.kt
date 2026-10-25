package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.EvaluacionRepository
import com.example.wim100.model.EstadoCandidatura
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DireccionViewModel(
    private val evaluacionRepository: EvaluacionRepository,
    private val candidataRepository: CandidataRepository
) : ViewModel() {

    // Se conecta al Flow del DAO que trae el ranking
    val rankingConsolidado = evaluacionRepository.observarConsolidado()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList() // Empieza como lista vacía mientras carga
        )

    // Función para que el Director presione "Seleccionada" o "No seleccionada"
    fun resolverCandidatura(candidataId: Long, nuevoEstado: EstadoCandidatura, nombreDirector: String) {
        viewModelScope.launch {
            candidataRepository.resolver(candidataId, nuevoEstado, nombreDirector)
        }
    }
}