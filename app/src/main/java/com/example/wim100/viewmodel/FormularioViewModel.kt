package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.remote.mensajeParaUsuario
import com.example.wim100.data.repository.RandomUserRepository
import com.example.wim100.data.repository.HistorialRepository
import com.example.wim100.model.Acciones
import retrofit2.HttpException
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.model.Candidata
import com.example.wim100.validation.CandidataValidator
import com.example.wim100.validation.FormularioValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FormularioViewModel(
    private val repository: CandidataRepository,
    private val randomUser: RandomUserRepository? = null,
    private val historial: HistorialRepository? = null
) : ViewModel() {
    private var original: Candidata? = null
    private var cargada: Long? = null

    fun cargar(id: Long) {
        if (cargada == id || _uiState.value.cargando) return
        _uiState.value = _uiState.value.copy(cargando = true, errorGuardado = null)
        viewModelScope.launch {
            try {
                val candidata = repository.obtener(id) ?: error("La candidata ya no está disponible")
                check(!candidata.estado.estaResuelta) { "La candidatura ya fue resuelta" }
                original = candidata
                cargada = id
                _uiState.value = FormularioUiState(
                    nombre = candidata.nombre, correo = candidata.correo, telefono = candidata.telefono,
                    area = candidata.area, aniosExperiencia = candidata.aniosExperiencia.toString(),
                    resumenExperiencia = candidata.resumenExperiencia, linkedinUrl = candidata.linkedinUrl,
                    cvUri = candidata.cvUri, cvNombre = candidata.cvNombre, esEdicion = true)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiState.value = _uiState.value.copy(cargando = false, errorGuardado = e.mensajeParaUsuario())
            }
        }
    }

    fun generarNombreFicticio() {
        if (_uiState.value.guardando || _uiState.value.generandoNombre || _uiState.value.idGuardado != null) return
        _uiState.value = _uiState.value.copy(generandoNombre = true, errorGuardado = null)
        viewModelScope.launch {
            try {
                val nombre = checkNotNull(randomUser).obtenerNombreAleatorio()
                actualizarNombre(nombre)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorGuardado = "No se pudo generar un nombre de ejemplo. Puedes escribirlo manualmente.")
            } finally { _uiState.value = _uiState.value.copy(generandoNombre = false) }
        }
    }
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
                cv = if (original != null && estado.cvUri == original?.cvUri && estado.cvNombre == original?.cvNombre) {
                    null
                } else if (estado.cvUri.isNullOrBlank()) {
                    "Adjunta el CV en PDF"
                } else {
                    CandidataValidator.validarCv(estado.cvNombre, estado.cvTamano).mensaje
                }
            )
            return estado.copy(errores = erroresCompletos, formularioValido = !erroresCompletos.hayErrores)
        }

        return estado.copy(errores = errores, formularioValido = !errores.hayErrores)
    }



    fun enviarPostulacion(usuario: String? = null) {
        if (_uiState.value.cargando || _uiState.value.generandoNombre || _uiState.value.guardando || _uiState.value.idGuardado != null || !validarPostulacion()) return
        if (_uiState.value.esEdicion && original == null) return

        val estado = _uiState.value
        val responsable = usuario?.trim()?.takeIf { it.isNotEmpty() } ?: estado.nombre.trim()
        _uiState.value = estado.copy(guardando = true, errorGuardado = null)

        viewModelScope.launch {
            try {
                val candidata = Candidata(
                    id = original?.id ?: 0L,
                    nombre = estado.nombre.trim(),
                    correo = estado.correo.trim().lowercase(),
                    telefono = CandidataValidator.formatearTelefono(estado.telefono),
                    area = estado.area,
                    aniosExperiencia = estado.aniosExperiencia.trim().toInt(),
                    resumenExperiencia = estado.resumenExperiencia.trim(),
                    linkedinUrl = estado.linkedinUrl.trim(),
                    cvUri = estado.cvUri,
                    cvNombre = estado.cvNombre,
                    estado = original?.estado ?: com.example.wim100.model.EstadoCandidatura.POSTULADA,
                    fechaRegistro = original?.fechaRegistro ?: System.currentTimeMillis(),
                    registradaPor = original?.registradaPor ?: responsable
                )
                val idGenerado = if (original == null) repository.registrarEnServidor(candidata)
                else {
                    repository.actualizarEnServidor(candidata)
                    candidata.id
                }
                var aviso: String? = null
                try {
                    historial?.registrar(idGenerado,
                        if (original == null) Acciones.REGISTRO else "Edición de candidatura",
                        if (original == null) "Se registró a ${candidata.nombre}" else "Se editaron los datos de ${candidata.nombre}",
                        responsable)
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { aviso = "Los datos se guardaron, pero no se pudo registrar el historial" }
                _uiState.value = _uiState.value.copy(guardando = false, idGuardado = idGenerado, aviso = aviso)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                _uiState.value = if (e.code() == 409) _uiState.value.copy(
                    guardando = false,
                    errores = _uiState.value.errores.copy(correo = "Ya existe una candidatura con este correo"),
                    formularioValido = false)
                else _uiState.value.copy(guardando = false, errorGuardado = e.mensajeParaUsuario())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(guardando = false, errorGuardado = e.mensajeParaUsuario())
            }
        }
    }

    fun limpiarFormulario() {
        if (_uiState.value.guardando) return
        intentoEnviar = false
        original = null
        cargada = null
        _uiState.value = FormularioUiState()
    }
}
