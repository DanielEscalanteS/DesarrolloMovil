package com.example.wim100.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wim100.WimApplication
import com.example.wim100.ui.screens.CandidatasScreen
import com.example.wim100.ui.screens.PostulacionScreen
import com.example.wim100.viewmodel.AppViewModelFactory

@Composable
fun WimApp() {
    val application = LocalContext.current.applicationContext as WimApplication
    val factory = remember(application) { AppViewModelFactory(application.container.candidataRepository) }
    val navController = rememberNavController()
    Scaffold { padding ->
        NavHost(navController, startDestination = "inicio", modifier = Modifier.padding(padding)) {
            composable("inicio") {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                    Text("WIM 100", style = MaterialTheme.typography.displaySmall)
                    Text("Postulación y revisión de antecedentes", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { navController.navigate("postulacion") }, modifier = Modifier.fillMaxWidth()) { Text("Quiero postular") }
                    OutlinedButton(onClick = { navController.navigate("candidatas") }, modifier = Modifier.fillMaxWidth()) { Text("Ver postulaciones") }
                    Spacer(Modifier.height(12.dp))
                    Text("Demostración académica con datos ficticios. Ambas vistas usan la misma base local.", style = MaterialTheme.typography.bodySmall)
                }
            }
            composable("postulacion") {
                PostulacionScreen(viewModel = viewModel(factory = factory), onVolver = { navController.popBackStack() })
            }
            composable("candidatas") {
                CandidatasScreen(viewModel = viewModel(factory = factory), onVolver = { navController.popBackStack() })
            }
        }
    }
}
