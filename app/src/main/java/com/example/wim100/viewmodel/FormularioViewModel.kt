package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.model.Candidata
import com.example.wim100.validation.CandidataValidator
import com.example.wim100.validation.FormularioValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FormularioViewModel(private val repository: CandidataRepository) : ViewModel() {
    private var intentoEnviar = false

    private val _uiState = MutableStateFlow(FormularioUiState())
    val uiState: StateFlow<FormularioUiState> = _uiState.asStateFlow()



    // FUNCIONES SIMPLES DE ACTUALIZACIÓN
    fun actualizarNombre(nombre: String) {
        val nuevoEstado = _uiState.value.copy(nombre = nombre)
        aplicarCambios(nuevoEstado)
    }

    fun actualizarCorreo(correo: String) {
        val nuevoEstado = _uiState.value.copy(correo = correo)
        aplicarCambios(nuevoEstado)
    }

    fun actualizarTelefono(telefono: String) {
        val nuevoEstado = _uiState.value.copy(telefono = telefono)
        aplicarCambios(nuevoEstado)
    }

    fun actualizarArea(area: String) {
        val nuevoEstado = _uiState.value.copy(area = area)
        aplicarCambios(nuevoEstado)
    }

    fun actualizarAnios(aniosExperiencia: String) {
        val nuevoEstado = _uiState.value.copy(aniosExperiencia = aniosExperiencia)
        aplicarCambios(nuevoEstado)
    }

    fun actualizarResumen(resumenExperiencia: String) {
        val nuevoEstado = _uiState.value.copy(resumenExperiencia = resumenExperiencia)
        aplicarCambios(nuevoEstado)
    }

    fun actualizarLinkedin(linkedinUrl: String) {
        val nuevoEstado = _uiState.value.copy(linkedinUrl = linkedinUrl)
        aplicarCambios(nuevoEstado)
    }

    fun actualizarCv(uri: String, nombre: String?, tamano: Long?) {
        val nuevoEstado = _uiState.value.copy(cvUri = uri, cvNombre = nombre, cvTamano = tamano)
        aplicarCambios(nuevoEstado)
    }

    fun informarErrorArchivo(mensaje: String) {
        _uiState.value = _uiState.value.copy(errorGuardado = mensaje)
    }



    // VALIDACIONES
    private fun aplicarCambios(estadoPropuesto: FormularioUiState) {
        if (_uiState.value.guardando || _uiState.value.idGuardado != null) return

        var estadoLimpio = estadoPropuesto.copy(errorGuardado = null)

        if (estadoLimpio.intentoContinuar) {
            estadoLimpio = validarEstado(estadoLimpio)
        } else {
            estadoLimpio = estadoLimpio.copy(formularioValido = false)
        }

        _uiState.value = estadoLimpio
    }

    fun validarFormulario(): Boolean {
        val estadoActual = _uiState.value
        val estadoConIntento = estadoActual.copy(intentoContinuar = true)

        _uiState.value = validarEstado(estadoConIntento)
        return _uiState.value.formularioValido
    }

    fun validarPostulacion(): Boolean {
        intentoEnviar = true
        return validarFormulario()
    }

    private fun validarEstado(estado: FormularioUiState): FormularioUiState {
        val errores = FormularioValidator.validarFormulario(
            estado.nombre,
            estado.correo,
            estado.telefono
        )

        if (intentoEnviar) {
            val erroresCompletos = errores.copy(
                area = CandidataValidator.validarArea(estado.area).mensaje,
                aniosExperiencia = CandidataValidator.validarAnios(estado.aniosExperiencia).mensaje,
                resumenExperiencia = CandidataValidator.validarResumen(estado.resumenExperiencia).mensaje,
                linkedinUrl = CandidataValidator.validarLinkedin(estado.linkedinUrl).mensaje,
                cv = if (estado.cvUri.isNullOrBlank()) {
                    "Adjunta el CV en PDF"
                } else {
                    CandidataValidator.validarCv(estado.cvNombre, estado.cvTamano).mensaje
                }
            )
            return estado.copy(errores = erroresCompletos, formularioValido = !erroresCompletos.hayErrores)
        }

        return estado.copy(errores = errores, formularioValido = !errores.hayErrores)
    }



    // CONEXIÓN CON LA BASE DE DATOS (Room)
    fun enviarPostulacion() {
        if (_uiState.value.guardando || _uiState.value.idGuardado != null || !validarPostulacion()) return

        val estado = _uiState.value
        _uiState.value = estado.copy(guardando = true, errorGuardado = null)

        viewModelScope.launch {
            try {
                val candidata = Candidata(
                    nombre = estado.nombre.trim(),
                    correo = estado.correo.trim().lowercase(),
                    telefono = CandidataValidator.formatearTelefono(estado.telefono),
                    area = estado.area,
                    aniosExperiencia = estado.aniosExperiencia.trim().toInt(),
                    resumenExperiencia = estado.resumenExperiencia.trim(),
                    linkedinUrl = estado.linkedinUrl.trim(),
                    cvUri = estado.cvUri,
                    cvNombre = estado.cvNombre,
                    registradaPor = estado.nombre.trim()
                )
                val idGenerado = repository.registrar(candidata)
                _uiState.value = _uiState.value.copy(guardando = false, idGuardado = idGenerado)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    guardando = false,
                    errorGuardado = e.message ?: "No se pudo guardar la postulación"
                )
            }
        }
    }

    fun limpiarFormulario() {
        if (_uiState.value.guardando) return
        intentoEnviar = false
        _uiState.value = FormularioUiState()
    }
}