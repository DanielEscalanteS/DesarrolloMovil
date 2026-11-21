package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.remote.mensajeParaUsuario
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.HistorialRepository
import com.example.wim100.model.Candidata
import com.example.wim100.model.Rol
import com.example.wim100.model.Sesion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DetalleCandidataUiState(
    val candidata: Candidata? = null,
    val cargando: Boolean = true,
    val eliminando: Boolean = false,
    val eliminada: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleCandidataViewModel(
    private val repository: CandidataRepository,
    private val historial: HistorialRepository? = null
) : ViewModel() {
    private val candidataId = MutableStateFlow<Long?>(null)
    private val eliminando = MutableStateFlow(false)
    private val eliminada = MutableStateFlow(false)
    private val errorOperacion = MutableStateFlow<String?>(null)
    private val candidata = candidataId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.observarPorId(id)
    }
    val uiState = combine(candidata, eliminando, eliminada, errorOperacion) { actual, guardando, borrada, mensaje ->
        DetalleCandidataUiState(actual, candidataId.value == null, guardando, borrada, mensaje)
    }.catch { emit(DetalleCandidataUiState(cargando = false, error = "No se pudo consultar la candidata")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetalleCandidataUiState())

    fun cargar(id: Long) { candidataId.value = id }

    fun eliminar(sesion: Sesion) {
        val id = candidataId.value ?: return
        if (eliminando.value || eliminada.value) return
        eliminando.value = true
        errorOperacion.value = null
        viewModelScope.launch {
            try {
                require(sesion.rol == Rol.EVALUADORA) { "Solo Evaluadora puede eliminar candidaturas" }
                val actual = repository.obtener(id) ?: error("La candidata ya no está disponible")
                check(!actual.estado.estaResuelta) { "La candidatura ya fue resuelta" }
                repository.eliminarEnServidor(id)
                try {
                    historial?.registrar(null, "Eliminación de candidatura", "Se eliminó a ${actual.nombre}", sesion.nombre)
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { errorOperacion.value = "Candidatura eliminada; no se pudo registrar el historial" }
                eliminada.value = true
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { errorOperacion.value = e.mensajeParaUsuario() }
            finally { eliminando.value = false }
        }
    }
}
