package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.local.CandidataConEvaluaciones
import com.example.wim100.data.repository.EvaluacionRepository
import com.example.wim100.model.ComparacionCandidatas
import com.example.wim100.model.Criterio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ComparacionUiState(
    val candidatas: List<CandidataConEvaluaciones> = emptyList(),
    val criterios: List<Criterio> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

class ComparacionViewModel(repository: EvaluacionRepository) : ViewModel() {
    private val seleccion = MutableStateFlow<List<Long>>(emptyList())
    val uiState = combine(repository.observarFichas(), repository.observarCriterios(), seleccion) { fichas, criterios, ids ->
        val candidatas = ids.mapNotNull { id -> fichas.find { it.candidata.id == id } }
        val error = if (candidatas.size != ids.size) "Alguna candidata ya no está disponible."
        else ComparacionCandidatas.validar(candidatas.map { it.candidata },
            candidatas.filter { it.evaluaciones.size == 1 }.map { it.candidata.id }.toSet())
        ComparacionUiState(candidatas, criterios.sortedBy { it.orden }, cargando = false, error = error)
    }.catch { emit(ComparacionUiState(cargando = false, error = "No se pudieron cargar los puntajes.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ComparacionUiState())

    fun cargar(ids: List<Long>) { seleccion.value = ids }
}
