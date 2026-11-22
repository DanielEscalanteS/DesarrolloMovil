package com.example.wim100.di

import android.content.Context
import com.example.wim100.data.local.WimDatabase
import com.example.wim100.data.repository.AuthRepository
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.DatosEjemploRepository
import com.example.wim100.data.repository.EvaluacionRepository
import com.example.wim100.data.repository.HistorialRepository
import com.example.wim100.data.repository.RandomUserRepository

class AppContainer(context: Context) {

    private val database by lazy {
        WimDatabase.obtener(context.applicationContext)
    }

    val historialRepository by lazy {
        HistorialRepository(database.historialDao())
    }

    val candidataRepository by lazy {
        CandidataRepository(
            database.candidataDao(),
            historialRepository
        )
    }

    val evaluacionRepository by lazy {
        EvaluacionRepository(database, historialRepository)
    }

    val datosEjemploRepository by lazy {
        DatosEjemploRepository(
            context.applicationContext,
            database,
            candidataRepository,
            evaluacionRepository,
            historialRepository
        )
    }

    val authRepository by lazy {
        AuthRepository()
    }

    val randomUserRepository by lazy {
        RandomUserRepository()
    }
}