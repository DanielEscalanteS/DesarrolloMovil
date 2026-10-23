package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.model.Candidata
import com.example.wim100.validation.CandidataValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FormularioViewModel(private val repository: CandidataRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FormularioUiState())
    val uiState = _uiState.asStateFlow()
    private var intentoEnviar = false

    fun actualizarNombre(valor: String) = actualizarEstado { it.copy(nombre = valor) }
    fun actualizarCorreo(valor: String) = actualizarEstado { it.copy(correo = valor) }
    fun actualizarTelefono(valor: String) = actualizarEstado { it.copy(telefono = valor) }
    fun actualizarArea(valor: String) = actualizarEstado { it.copy(area = valor) }
    fun actualizarAnios(valor: String) = actualizarEstado { it.copy(aniosExperiencia = valor) }
    fun actualizarResumen(valor: String) = actualizarEstado { it.copy(resumenExperiencia = valor) }
    fun actualizarLinkedin(valor: String) = actualizarEstado { it.copy(linkedinUrl = valor) }
    fun actualizarCv(uri: String, nombre: String?, tamano: Long?) = actualizarEstado {
        it.copy(cvUri = uri, cvNombre = nombre, cvTamano = tamano)
    }
    fun informarErrorArchivo(mensaje: String) = _uiState.update { it.copy(errorGuardado = mensaje) }

    private fun actualizarEstado(cambio: (FormularioUiState) -> FormularioUiState) {
        if (_uiState.value.guardando || _uiState.value.idGuardado != null) return
        _uiState.update { estado ->
            val nuevo = cambio(estado).copy(errorGuardado = null)
            when {
                intentoEnviar -> validarEstado(nuevo, completa = true)
                nuevo.intentoContinuar -> validarEstado(nuevo, completa = false)
                else -> nuevo.copy(formularioValido = false)
            }
        }
    }

    /** Mantiene la comprobación de datos personales que ya tenía el equipo. */
    fun validarFormulario(): Boolean {
        _uiState.update { validarEstado(it.copy(intentoContinuar = true), completa = intentoEnviar) }
        return _uiState.value.formularioValido
    }

    fun validarPostulacion(): Boolean {
        intentoEnviar = true
        _uiState.update { validarEstado(it.copy(intentoContinuar = true), completa = true) }
        return _uiState.value.formularioValido
    }

    private fun validarEstado(s: FormularioUiState, completa: Boolean): FormularioUiState {
        var errores = FormularioValidator.validarFormulario(s.nombre, s.correo, s.telefono)
        if (completa) {
            errores = errores.copy(
                area = CandidataValidator.validarArea(s.area).mensaje,
                aniosExperiencia = CandidataValidator.validarAnios(s.aniosExperiencia).mensaje,
                resumenExperiencia = CandidataValidator.validarResumen(s.resumenExperiencia).mensaje,
                linkedinUrl = CandidataValidator.validarLinkedin(s.linkedinUrl).mensaje,
                cv = if (s.cvUri.isNullOrBlank()) "Adjunta el CV en PDF" else
                    CandidataValidator.validarCv(s.cvNombre, s.cvTamano).mensaje
            )
        }
        return s.copy(errores = errores, formularioValido = !errores.hayErrores)
    }

    fun enviarPostulacion() {
        if (_uiState.value.guardando || _uiState.value.idGuardado != null || !validarPostulacion()) return
        val s = _uiState.value
        _uiState.update { it.copy(guardando = true, errorGuardado = null) }
        viewModelScope.launch {
            try {
                val id = repository.registrar(Candidata(
                    nombre = s.nombre.trim(), correo = s.correo.trim().lowercase(),
                    telefono = CandidataValidator.formatearTelefono(s.telefono), area = s.area,
                    aniosExperiencia = s.aniosExperiencia.trim().toInt(),
                    resumenExperiencia = s.resumenExperiencia.trim(), linkedinUrl = s.linkedinUrl.trim(),
                    cvUri = s.cvUri, cvNombre = s.cvNombre, registradaPor = s.nombre.trim()
                ))
                _uiState.update { it.copy(guardando = false, idGuardado = id) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(guardando = false, errorGuardado = e.message ?: "No se pudo guardar la postulación") }
            }
        }
    }

    fun limpiarFormulario() {
        if (_uiState.value.guardando) return
        intentoEnviar = false
        _uiState.value = FormularioUiState()
    }
}
