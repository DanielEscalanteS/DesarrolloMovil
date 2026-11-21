package com.example.wim100.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wim100.WimApplication
import com.example.wim100.navigation.WimNavHost
import com.example.wim100.navigation.AccesoNavHost
import com.example.wim100.ui.viewmodel.AppViewModelProvider
import com.example.wim100.viewmodel.SesionViewModel

@Composable
fun WimApp() {
    val application = LocalContext.current.applicationContext as WimApplication
    val factory = remember(application) {
        AppViewModelProvider(
            candidataRepository = application.container.candidataRepository,
            evaluacionRepository = application.container.evaluacionRepository,
            historialRepository = application.container.historialRepository,
            datosEjemploRepository = application.container.datosEjemploRepository,
            authRepository = application.container.authRepository,
            randomUserRepository = application.container.randomUserRepository
        )
    }
    val sesionViewModel: SesionViewModel = viewModel(factory = factory)
    val sesion by sesionViewModel.sesion.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val actual = sesion
            if (actual == null) AccesoNavHost(factory, sesionViewModel)
            else key(actual.id, actual.usuario) {
                WimNavHost(actual, factory, sesionViewModel::cerrar)
            }
        }
    }
}
