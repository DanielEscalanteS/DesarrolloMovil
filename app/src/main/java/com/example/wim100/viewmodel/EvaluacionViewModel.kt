package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.remote.mensajeParaUsuario
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.EvaluacionRepository
import com.example.wim100.model.CalculadoraPuntaje
import com.example.wim100.model.Criterio
import com.example.wim100.model.CriteriosDemostracion
import com.example.wim100.model.EstadoCandidatura
import com.example.wim100.model.Evaluacion
import com.example.wim100.model.Rol
import com.example.wim100.model.Sesion
import com.example.wim100.validation.EvaluacionValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EvaluacionUiState(
    val criterios: List<Criterio> = emptyList(),
    val puntajes: Map<Long, Int> = emptyMap(),
    val observaciones: Map<Long, String> = emptyMap(),
    val observacionGeneral: String = "",
    val errorGeneral: String? = null,
    val cargando: Boolean = true,
    val puedeGuardar: Boolean = false,
    val editando: Boolean = false,
    val revision: Int = 0,
    val candidataNombre: String = "",
    val guardando: Boolean = false,
    val pendienteSincronizar: Boolean = false,
    val finalizadoConExito: Boolean = false
) {
    val puntajeTotal: Double get() = CalculadoraPuntaje.calcular(criterios, puntajes)
}

class EvaluacionViewModel(
    private val repository: EvaluacionRepository,
    private val candidatas: CandidataRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(EvaluacionUiState())
    val uiState: StateFlow<EvaluacionUiState> = _uiState.asStateFlow()
    private var candidataCargada: Long? = null

    fun cargarCriterios(candidataId: Long, sesion: Sesion) {
        if (candidataCargada == candidataId) return
        candidataCargada = candidataId
        viewModelScope.launch {
            try {
                val criteriosOficiales = CriteriosDemostracion.lista
                repository.asegurarCriterios(criteriosOficiales)

                val lista = repository.obtenerCriterios()
                val ficha = repository.obtenerFicha(candidataId) ?: error("La candidatura no existe")
                check(sesion.rol == Rol.EVALUADORA) { "Acceso reservado a Evaluadora" }
                check(!ficha.candidata.estado.estaResuelta) { "Dirección ya resolvió esta candidatura" }
                val anterior = ficha.evaluaciones.singleOrNull()
                if (anterior != null) {
                    check(anterior.evaluacion.evaluadoraClave == Evaluacion.normalizarClave(sesion.nombre)) {
                        "Solo ${anterior.evaluacion.evaluadora} puede editar esta evaluación"
                    }
                }
                _uiState.update {
                    it.copy(
                        criterios = lista,
                        cargando = false,
                        puedeGuardar = true,
                        candidataNombre = ficha.candidata.nombre,
                        editando = anterior != null,
                        revision = anterior?.evaluacion?.revision ?: 0,
                        puntajes = anterior?.puntajes?.associate { p -> p.criterioId to p.puntaje }.orEmpty(),
                        observaciones = anterior?.puntajes?.associate { p -> p.criterioId to p.observacion }.orEmpty(),
                        observacionGeneral = anterior?.evaluacion?.observacionGeneral.orEmpty()
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        cargando = false,
                        puedeGuardar = false,
                        errorGeneral = e.message ?: "No se pudo cargar la evaluación"
                    )
                }
            }
        }
    }

    fun actualizarPuntaje(criterioId: Long, nota: Int) {
        if (_uiState.value.guardando || _uiState.value.pendienteSincronizar) return
        _uiState.update { estado ->
            val nuevosPuntajes = estado.puntajes + (criterioId to nota)
            estado.copy(puntajes = nuevosPuntajes)
        }
    }

    fun actualizarObservacion(criterioId: Long, texto: String) {
        if (_uiState.value.guardando || _uiState.value.pendienteSincronizar) return
        _uiState.update { it.copy(observaciones = it.observaciones + (criterioId to texto)) }
    }

    fun actualizarObservacionGeneral(texto: String) {
        if (_uiState.value.guardando || _uiState.value.pendienteSincronizar) return
        _uiState.update { it.copy(observacionGeneral = texto) }
    }

    fun guardarEvaluacion(candidataId: Long, sesion: Sesion) {
        if (_uiState.value.guardando || _uiState.value.finalizadoConExito || !_uiState.value.puedeGuardar) return
        val estado = _uiState.value
        if (estado.pendienteSincronizar) {
            sincronizarEstado(candidataId, sesion)
            return
        }
        if (estado.criterios.isEmpty()) {
            _uiState.update { it.copy(errorGeneral = "Espera a que carguen los criterios") }
            return
        }
        for (criterio in estado.criterios) {
            val nota = EvaluacionValidator.validarPuntaje(estado.puntajes[criterio.id])
            val observacion = EvaluacionValidator.validarObservacionCriterio(
                estado.puntajes[criterio.id], estado.observaciones[criterio.id].orEmpty()
            )
            if (!nota.esValido || !observacion.esValido) {
                _uiState.update { it.copy(errorGeneral = "${criterio.nombre}: ${nota.mensaje ?: observacion.mensaje}") }
                return
            }
        }

        val validacionGeneral = EvaluacionValidator.validarObservacionGeneral(estado.observacionGeneral)
        if (!validacionGeneral.esValido) {
            _uiState.update { it.copy(errorGeneral = validacionGeneral.mensaje) }
            return
        }

        _uiState.update { it.copy(guardando = true, errorGeneral = null) }
        viewModelScope.launch {
            try {
                if (estado.editando) {
                    repository.actualizar(
                        candidataId, sesion, estado.revision, estado.puntajes,
                        estado.observaciones, estado.observacionGeneral
                    )
                } else {
                    repository.registrar(
                        candidataId = candidataId,
                        evaluadora = sesion.nombre,
                        puntajes = estado.puntajes,
                        observaciones = estado.observaciones,
                        observacionGeneral = estado.observacionGeneral
                    )
                }
                _uiState.update { it.copy(guardando = false, pendienteSincronizar = true) }
                sincronizarEstado(candidataId, sesion)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(guardando = false, errorGeneral = e.message) }
            }
        }
    }

    private fun sincronizarEstado(candidataId: Long, sesion: Sesion) {
        val estado = _uiState.value
        _uiState.update { it.copy(guardando = true, errorGeneral = null) }
        viewModelScope.launch {
            try {
                val candidata = candidatas.obtener(candidataId) ?: error("La candidatura no existe")
                check(!candidata.estado.estaResuelta) { "Dirección ya resolvió esta candidatura" }
                repository.enviarAlServidor(
                    candidataId = candidataId,
                    evaluadora = sesion.nombre,
                    puntajes = estado.puntajes,
                    observaciones = estado.observaciones,
                    observacionGeneral = estado.observacionGeneral
                )
                candidatas.cambiarEstadoEnServidor(candidataId, EstadoCandidatura.EVALUADA)
                _uiState.update { it.copy(guardando = false, pendienteSincronizar = false, finalizadoConExito = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        guardando = false,
                        errorGeneral = "La evaluación quedó guardada en este dispositivo. Falta sincronizar con el servidor: ${e.mensajeParaUsuario()}"
                    )
                }
            }
        }
    }
}