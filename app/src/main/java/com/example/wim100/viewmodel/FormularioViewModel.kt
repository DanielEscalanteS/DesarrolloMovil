package com.example.wim100.viewmodel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.model.Candidata
import com.example.wim100.validation.CandidataValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FormularioViewModel(private val repository: CandidataRepository) : ViewModel() {
    private var intentoEnviar = false
    private val _uiState = MutableStateFlow(FormularioUiState())
    val uiState: StateFlow<FormularioUiState> =
        _uiState.asStateFlow()
    fun actualizarNombre(nombre: String) {
        actualizarEstado { it.copy(nombre = nombre) }
    }
    fun actualizarCorreo(correo: String) {
        actualizarEstado { it.copy(correo = correo) }
    }
    fun actualizarTelefono(telefono: String) {
        actualizarEstado { it.copy(telefono = telefono) }
    }
    fun actualizarArea(area: String) {
        actualizarEstado { it.copy(area = area) }
    }
    fun actualizarAnios(aniosExperiencia: String) {
        actualizarEstado { it.copy(aniosExperiencia = aniosExperiencia) }
    }
    fun actualizarResumen(resumenExperiencia: String) {
        actualizarEstado { it.copy(resumenExperiencia = resumenExperiencia) }
    }
    fun actualizarLinkedin(linkedinUrl: String) {
        actualizarEstado { it.copy(linkedinUrl = linkedinUrl) }
    }
    fun actualizarCv(uri: String, nombre: String?, tamano: Long?) {
        actualizarEstado {
            it.copy(cvUri = uri, cvNombre = nombre, cvTamano = tamano)
        }
    }
    fun informarErrorArchivo(mensaje: String) {
        _uiState.update { it.copy(errorGuardado = mensaje) }
    }
    private fun actualizarEstado(
        cambio: (FormularioUiState) -> FormularioUiState
    ) {
        if (_uiState.value.guardando || _uiState.value.idGuardado != null) return
        _uiState.update { estado ->
            val nuevoEstado = cambio(estado).copy(errorGuardado = null)

            if (nuevoEstado.intentoContinuar) {
                validarEstado(nuevoEstado)
            } else {
                nuevoEstado.copy(formularioValido = false)
            }
        }
    }
    fun validarFormulario(): Boolean {
        _uiState.update { estado ->
            validarEstado(
                estado.copy(intentoContinuar = true)
            )
        }

        return _uiState.value.formularioValido
    }
    fun validarPostulacion(): Boolean {
        intentoEnviar = true
        return validarFormulario()
    }
    private fun validarEstado(
        estado: FormularioUiState
    ): FormularioUiState {
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
            return estado.copy(
                errores = erroresCompletos,
                formularioValido = !erroresCompletos.hayErrores
            )
        }
        return estado.copy(
            errores = errores,
            formularioValido = !errores.hayErrores
        )
    }
    fun enviarPostulacion() {
        if (_uiState.value.guardando || _uiState.value.idGuardado != null || !validarPostulacion()) return
        val estado = _uiState.value
        _uiState.update { it.copy(guardando = true, errorGuardado = null) }
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
                _uiState.update {
                    it.copy(guardando = false, idGuardado = idGenerado)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        guardando = false,
                        errorGuardado = e.message ?: "No se pudo guardar la postulación"
                    )
                }
            }
        }
    }

    fun limpiarFormulario() {
        if (_uiState.value.guardando) return
        intentoEnviar = false
        _uiState.value = FormularioUiState()
    }
}
