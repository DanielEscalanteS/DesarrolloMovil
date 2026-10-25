package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.EvaluacionRepository
import com.example.wim100.model.EstadoCandidatura
import com.example.wim100.model.Sesion
import com.example.wim100.model.ComparacionCandidatas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FiltrosDireccion(
    val estado: EstadoCandidatura? = null,
    val area: String? = null,
    val criterioId: Long? = null,
    val ascendente: Boolean = false
)

class DireccionViewModel(
    private val evaluacionRepository: EvaluacionRepository,
    private val candidataRepository: CandidataRepository
) : ViewModel() {

    private val _guardando = MutableStateFlow(false)
    val guardando = _guardando.asStateFlow()
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje = _mensaje.asStateFlow()
    private val _cargando = MutableStateFlow(true)
    val cargando = _cargando.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _filtros = MutableStateFlow(FiltrosDireccion())
    val filtros = _filtros.asStateFlow()
    private val _seleccionadas = MutableStateFlow<List<Long>>(emptyList())
    val seleccionadas = _seleccionadas.asStateFlow()
    val criterios = evaluacionRepository.observarCriterios()
        .catch { _error.value = "No se pudieron cargar los criterios"; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val fichas = evaluacionRepository.observarFichas()
        .catch { _error.value = "No se pudieron consultar los puntajes"; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Se conecta al Flow del DAO que trae el ranking
    val rankingConsolidado = evaluacionRepository.observarConsolidado()
        .onEach { _cargando.value = false }
        .catch {
            _cargando.value = false
            _error.value = "No se pudieron consultar los resultados"
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList() // Empieza como lista vacía mientras carga
        )

    val listado = combine(rankingConsolidado, fichas, filtros) { ranking, detalles, filtro ->
        val filtradas = ranking.filter { (filtro.estado == null || it.estado == filtro.estado) &&
            (filtro.area == null || it.area == filtro.area) }
        val puntajes = if (filtro.criterioId == null) ranking.associate { it.candidataId to it.promedio }
        else detalles.associate { ficha -> ficha.candidata.id to ficha.evaluaciones.singleOrNull()
            ?.puntajes?.find { it.criterioId == filtro.criterioId }?.puntaje?.toDouble() }
        ComparacionCandidatas.ordenar(filtradas, puntajes, filtro.ascendente)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun filtrar(valor: FiltrosDireccion) { _filtros.value = valor }

    fun alternarComparacion(id: Long) {
        val seleccion = _seleccionadas.value
        if (id in seleccion) { _seleccionadas.value = seleccion - id; return }
        val ficha = fichas.value.find { it.candidata.id == id } ?: return
        if (ficha.evaluaciones.size != 1) { _mensaje.value = "Esta candidata todavía no tiene una evaluación."; return }
        if (seleccion.size >= ComparacionCandidatas.MAXIMO) { _mensaje.value = "Puedes comparar hasta tres candidatas."; return }
        val area = fichas.value.find { it.candidata.id == seleccion.firstOrNull() }?.candidata?.area
        if (area != null && area != ficha.candidata.area) {
            _mensaje.value = "Elige candidatas de $area o limpia la selección para cambiar de área."
            return
        }
        _mensaje.value = null
        _seleccionadas.value = seleccion + id
    }

    fun limpiarSeleccion() { _seleccionadas.value = emptyList(); _mensaje.value = null }

    // Función para que el Director presione "Seleccionada" o "No seleccionada"
    fun resolverCandidatura(candidataId: Long, nuevoEstado: EstadoCandidatura, sesion: Sesion) {
        if (_guardando.value) return
        _guardando.value = true
        viewModelScope.launch {
            try {
                candidataRepository.resolver(candidataId, nuevoEstado, sesion)
                _mensaje.value = "Resolución guardada"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _mensaje.value = e.message ?: "No se pudo guardar la resolución"
            } finally {
                _guardando.value = false
            }
        }
    }
}
