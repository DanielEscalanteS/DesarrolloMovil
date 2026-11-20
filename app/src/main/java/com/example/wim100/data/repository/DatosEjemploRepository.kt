package com.example.wim100.data.repository

import android.content.Context
import com.example.wim100.data.local.DatosSemilla
import com.example.wim100.data.local.WimDatabase

class DatosEjemploRepository(
    context: Context,
    private val db: WimDatabase,
    private val candidatas: CandidataRepository,
    evaluaciones: EvaluacionRepository,
    historial: HistorialRepository
) {
    companion object {
        const val RESPONSABLE = "Ejemplos WIM100"
        const val PREPARACION = "Ejemplos iniciales v2"
    }

    suspend fun cargar() {
        DatosSemilla.preparar(db)
        candidatas.sincronizarDesdeServidor()
    }
}
