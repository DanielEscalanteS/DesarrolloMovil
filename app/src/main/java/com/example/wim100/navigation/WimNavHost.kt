package com.example.wim100.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun WimNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Rutas.LOGIN
    ) {
        composable(Rutas.LOGIN) {
            Text("Login")
        }

        composable(Rutas.CANDIDATAS) {
            Text("Candidatas")
        }

        composable(Rutas.CANDIDATA_NUEVA) {
            Text("Nueva candidata")
        }

        composable(Rutas.CANDIDATA_DETALLE) {
            Text("Detalle candidata")
        }

        composable(Rutas.CANDIDATA_EDITAR) {
            Text("Editar candidata")
        }

        composable(Rutas.EVALUACION) {
            Text("Evaluación")
        }

        composable(Rutas.DIRECCION) {
            Text("Dirección")
        }

        composable(Rutas.HISTORIAL) {
            Text("Historial")
        }

        composable(Rutas.DETALLE_EVALUACIONES) {
            Text("Detalle evaluaciones")
        }

        composable(Rutas.COMPARAR) {
            Text("Comparar candidatas")
        }
    }
}