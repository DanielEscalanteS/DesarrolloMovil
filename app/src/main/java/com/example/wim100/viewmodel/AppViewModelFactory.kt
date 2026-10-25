package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.EvaluacionRepository
import com.example.wim100.data.repository.HistorialRepository

class AppViewModelFactory(
    private val candidataRepository: CandidataRepository,
    private val evaluacionRepository: EvaluacionRepository,
    private val historialRepository: HistorialRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val instancia = when (modelClass) {
            FormularioViewModel::class.java -> FormularioViewModel(candidataRepository)
            CandidatasViewModel::class.java -> CandidatasViewModel(candidataRepository)
            EvaluacionViewModel::class.java -> EvaluacionViewModel(evaluacionRepository)
            DireccionViewModel::class.java -> DireccionViewModel(evaluacionRepository, candidataRepository)
            SesionViewModel::class.java -> SesionViewModel(historialRepository)
            HistorialViewModel::class.java -> HistorialViewModel(historialRepository)
            else -> error("ViewModel no registrado: ${modelClass.name}")
        }
        return modelClass.cast(instancia)!!
    }
}