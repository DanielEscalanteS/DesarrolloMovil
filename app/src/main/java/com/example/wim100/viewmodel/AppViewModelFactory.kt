package com.example.wim100.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wim100.data.repository.CandidataRepository

class AppViewModelFactory(private val repository: CandidataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val instancia = when (modelClass) {
            FormularioViewModel::class.java -> FormularioViewModel(repository)
            CandidatasViewModel::class.java -> CandidatasViewModel(repository)
            else -> error("ViewModel no registrado: ${modelClass.name}")
        }
        return modelClass.cast(instancia)!!
    }
}
