package com.example.wim100

import android.content.Context
import com.example.wim100.data.local.WimDatabase
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.EvaluacionRepository
import com.example.wim100.data.repository.HistorialRepository

class AppContainer(context: Context) {
    private val database by lazy { WimDatabase.obtener(context.applicationContext) }
    val historialRepository by lazy { HistorialRepository(database.historialDao()) }
    val candidataRepository by lazy { CandidataRepository(database.candidataDao(), historialRepository) }
    val evaluacionRepository by lazy { EvaluacionRepository(database, historialRepository) }
}
