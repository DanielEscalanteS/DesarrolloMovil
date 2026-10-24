package com.example.wim100.di

import android.content.Context
import com.example.wim100.data.local.WimDatabase
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.EvaluacionRepository
import com.example.wim100.data.repository.HistorialRepository

/** Dependencias compartidas. La conexión se abre de forma diferida al utilizarla. */
class AppContainer(context: Context) {
    private val database by lazy { WimDatabase.obtener(context.applicationContext) }
    val historialRepository by lazy { HistorialRepository(database.historialDao()) }
    val candidataRepository by lazy { CandidataRepository(database.candidataDao(), historialRepository) }
    val evaluacionRepository by lazy { EvaluacionRepository(database, historialRepository) }
}
