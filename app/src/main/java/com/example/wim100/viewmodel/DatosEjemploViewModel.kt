package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wim100.data.repository.DatosEjemploRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DatosEjemploUiState(val cargando: Boolean = true, val error: String? = null)

class DatosEjemploViewModel(private val repository: DatosEjemploRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(DatosEjemploUiState())
    val uiState = _uiState.asStateFlow()
    private var preparando = false

    init { preparar() }

    fun preparar() {
        if (preparando) return
        preparando = true
        _uiState.value = DatosEjemploUiState()
        viewModelScope.launch {
            try {
                repository.cargar()
                _uiState.value = DatosEjemploUiState(cargando = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = DatosEjemploUiState(cargando = false, error = "No se pudo preparar la aplicación. ${e.message.orEmpty()}")
            } finally { preparando = false }
        }
    }
}
