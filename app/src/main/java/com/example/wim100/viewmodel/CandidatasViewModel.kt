package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.EvaluacionRepository
import com.example.wim100.model.Candidata
import com.example.wim100.model.Evaluacion
import com.example.wim100.data.remote.mensajeParaUsuario
import com.example.wim100.model.CriteriosDemostracion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CandidatasUiState(
    val candidatas: List<Candidata> = emptyList(),
    val evaluaciones: List<Evaluacion> = emptyList(),
    val cargando: Boolean = true,
    val refrescando: Boolean = false,
    val error: String? = null
) {
    fun yaEvaluo(candidataId: Long, nombre: String): Boolean = evaluaciones.any {
        it.candidataId == candidataId && it.evaluadoraClave == Evaluacion.normalizarClave(nombre)
    }

    fun totalEvaluaciones(candidataId: Long): Int = evaluaciones.count { it.candidataId == candidataId }
}

class CandidatasViewModel(
    private val repository: CandidataRepository,
    private val evaluacionRepository: EvaluacionRepository
) : ViewModel() {
    private val refrescando = MutableStateFlow(false)
    private val errorRemoto = MutableStateFlow<String?>(null)

    init { refrescar() }

    fun refrescar() {
        if (refrescando.value) return
        refrescando.value = true
        errorRemoto.value = null
        viewModelScope.launch {
            try {
                evaluacionRepository.asegurarCriterios(CriteriosDemostracion.lista)
                repository.sincronizarDesdeServidor()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { errorRemoto.value = e.mensajeParaUsuario() }
            finally { refrescando.value = false }
        }
    }
    val uiState = combine(repository.observarTodas(), evaluacionRepository.observarTodas(), refrescando, errorRemoto) { candidatas, evaluaciones, actualizando, error ->
        CandidatasUiState(candidatas = candidatas, evaluaciones = evaluaciones, cargando = false, refrescando = actualizando, error = error)
    }
        .catch { emit(CandidatasUiState(cargando = false, error = "No se pudieron cargar las postulaciones")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CandidatasUiState())
}
