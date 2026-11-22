package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.remote.mensajeParaUsuario
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.EvaluacionRepository
import com.example.wim100.data.repository.HistorialRepository
import com.example.wim100.model.Acciones
import com.example.wim100.model.ComparacionCandidatas
import com.example.wim100.model.CriteriosDemostracion
import com.example.wim100.model.EstadoCandidatura
import com.example.wim100.model.Evaluacion
import com.example.wim100.model.Rol
import com.example.wim100.model.Sesion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
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
    private val candidataRepository: CandidataRepository,
    private val historialRepository: HistorialRepository
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
            initialValue = emptyList()
        )

    val listado = combine(rankingConsolidado, fichas, filtros) { ranking, detalles, filtro ->
        val filtradas = ranking.filter {
            (filtro.estado == null || it.estado == filtro.estado) &&
                    (filtro.area == null || it.area == filtro.area)
        }
        val puntajes = if (filtro.criterioId == null) {
            ranking.associate { it.candidataId to it.promedio }
        } else {
            detalles.associate { ficha ->
                ficha.candidata.id to ficha.evaluaciones.singleOrNull()
                    ?.puntajes?.find { it.criterioId == filtro.criterioId }?.puntaje?.toDouble()
            }
        }
        ComparacionCandidatas.ordenar(filtradas, puntajes, filtro.ascendente)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var datosActualizados = false
    private var refrescando = false

    fun refrescar() {
        if (refrescando || _guardando.value) return
        refrescando = true
        datosActualizados = false
        _guardando.value = true
        viewModelScope.launch {
            try {
                evaluacionRepository.asegurarCriterios(CriteriosDemostracion.lista)
                candidataRepository.sincronizarDesdeServidor()
                evaluacionRepository.sincronizarDesdeServidor()
                datosActualizados = true
                _mensaje.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _mensaje.value = e.mensajeParaUsuario()
            } finally {
                refrescando = false
                _guardando.value = false
            }
        }
    }

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

    fun resolverCandidatura(candidataId: Long, nuevoEstado: EstadoCandidatura, sesion: Sesion) {
        if (_guardando.value) return
        _guardando.value = true
        viewModelScope.launch {
            try {
                check(datosActualizados) { "Refresca los postulantes antes de resolver una candidatura" }
                require(sesion.rol == Rol.DIRECCION) { "Solo Dirección puede resolver candidaturas" }
                require(nuevoEstado.estaResuelta) { "Selecciona una resolución válida" }
                val ficha = evaluacionRepository.obtenerFicha(candidataId) ?: error("La candidatura no existe")
                check(
                    ficha.candidata.estado == EstadoCandidatura.EVALUADA &&
                            ficha.evaluaciones.size >= Evaluacion.MINIMO_PARA_CONSOLIDAR
                ) {
                    "La candidatura debe estar evaluada y no resuelta"
                }
                candidataRepository.cambiarEstadoEnServidor(candidataId, nuevoEstado)
                try {
                    historialRepository.registrar(
                        candidataId, Acciones.RESOLUCION,
                        "Candidatura marcada como ${nuevoEstado.etiqueta}", sesion.nombre
                    )
                    _mensaje.value = "Resolución guardada"
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _mensaje.value = "Resolución guardada; no se pudo registrar el historial"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _mensaje.value = e.mensajeParaUsuario()
            } finally {
                _guardando.value = false
            }
        }
    }
}