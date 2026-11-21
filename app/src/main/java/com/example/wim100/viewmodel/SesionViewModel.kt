package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.remote.mensajeParaUsuario
import com.example.wim100.data.repository.AuthRepository
import com.example.wim100.data.repository.HistorialRepository
import com.example.wim100.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val usuario: String = "",
    val password: String = "",
    val errorUsuario: String? = null,
    val errorPassword: String? = null,
    val error: String? = null,
    val cargando: Boolean = false,
    val requiereBiometria: Boolean = false
)

class SesionViewModel(
    private val historial: HistorialRepository,
    private val auth: AuthRepository
) : ViewModel() {
    private val _sesion = MutableStateFlow<Sesion?>(null)
    val sesion = _sesion.asStateFlow()
    private val _login = MutableStateFlow(LoginUiState())
    val loginState = _login.asStateFlow()
    private var pendiente: Sesion? = null
    private var acceso: Job? = null

    fun onUsuario(valor: String) {
        if (_login.value.cargando) return
        _login.update { it.copy(usuario = valor, errorUsuario = null, error = null) }
    }

    fun onPassword(valor: String) {
        if (_login.value.cargando) return
        _login.update { it.copy(password = valor, errorPassword = null, error = null) }
    }

    fun iniciarSesion() {
        if (_login.value.cargando || _sesion.value != null) return
        val datos = _login.value
        val usuarioError = if (datos.usuario.isBlank()) "Ingresa tu usuario" else null
        val passwordError = if (datos.password.isBlank()) "Ingresa tu contraseña" else null
        _login.update { it.copy(errorUsuario = usuarioError, errorPassword = passwordError, error = null) }
        if (usuarioError != null || passwordError != null) return
        _login.update { it.copy(cargando = true) }
        acceso = viewModelScope.launch {
            try {
                val actual = auth.iniciarSesion(datos.usuario.trim(), datos.password)
                if (actual.rol == Rol.DIRECCION) {
                    pendiente = actual
                    _login.update { it.copy(password = "", requiereBiometria = true) }
                } else completarAcceso(actual)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                pendiente = null
                _login.update { it.copy(cargando = false, error = e.mensajeParaUsuario()) }
            }
        }
    }

    fun confirmarBiometria() {
        val actual = pendiente ?: return
        pendiente = null
        _login.update { it.copy(requiereBiometria = false) }
        acceso = viewModelScope.launch { completarAcceso(actual) }
    }

    fun cancelarBiometria(mensaje: String) {
        if (pendiente == null) return
        pendiente = null
        _login.update { it.copy(cargando = false, requiereBiometria = false, error = mensaje) }
    }

    private suspend fun completarAcceso(actual: Sesion) {
        try {
            historial.registrar(null, Acciones.INICIO_SESION, "Ingreso como ${actual.rol.etiqueta}", actual.nombre)
            _login.value = LoginUiState()
            _sesion.value = actual
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            _login.update { it.copy(cargando = false, requiereBiometria = false,
                error = "No se pudo registrar el acceso. Intenta nuevamente.") }
        }
    }

    fun cerrar() {
        acceso?.cancel()
        pendiente = null
        val actual = _sesion.value
        _sesion.value = null
        _login.value = LoginUiState()
        if (actual != null) viewModelScope.launch {
            try {
                historial.registrar(null, Acciones.CIERRE_SESION, "Cierre de sesión", actual.nombre)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _login.update { it.copy(error = "La sesión se cerró, pero no se pudo registrar la salida") }
            }
        }
    }
}
