package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.repository.HistorialRepository
import com.example.wim100.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SesionViewModel(private val historial: HistorialRepository) : ViewModel() {
    private val _sesion = MutableStateFlow<Sesion?>(null)
    val sesion = _sesion.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun iniciar(nombre: String, rol: Rol, biometriaValidada: Boolean = false) {
        val nombreLimpio = nombre.trim().replace(Regex("\\s+"), " ")
        if (nombreLimpio.length < 3) {
            _error.value = "Ingresa un nombre de prueba de al menos 3 caracteres"
            return
        }
        if (rol == Rol.DIRECCION && !biometriaValidada) {
            _error.value = "Dirección requiere la verificación del dispositivo"
            return
        }
        viewModelScope.launch {
            try {
                historial.registrar(null, Acciones.INICIO_SESION, "Ingreso como ${rol.etiqueta}", nombreLimpio)
                _error.value = null
                _sesion.value = Sesion(nombreLimpio, rol)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = "No se pudo registrar el acceso. Intenta nuevamente." }
        }
    }

    fun cerrar() {
        val actual = _sesion.value ?: return
        _sesion.value = null
        viewModelScope.launch {
            try {
                historial.registrar(null, Acciones.CIERRE_SESION, "Cierre de sesión", actual.nombre)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = "La sesión se cerró, pero no se pudo registrar la salida" }
        }
    }
}
