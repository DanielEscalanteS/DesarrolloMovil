package com.example.wim100.ui

import android.widget.Toast
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
import com.example.wim100.nativo.BiometriaHelper
import com.example.wim100.ui.screens.CandidatasScreen
import com.example.wim100.ui.screens.PostulacionScreen
import com.example.wim100.ui.screens.DireccionScreen
import com.example.wim100.ui.screens.EvaluacionScreen
import com.example.wim100.viewmodel.AppViewModelFactory

@Composable
fun WimApp() {
    val context = LocalContext.current
    val application = context.applicationContext as WimApplication

    val factory = remember(application) {
        AppViewModelFactory(
            candidataRepository = application.container.candidataRepository,
            evaluacionRepository = application.container.evaluacionRepository
        )
    }

    val navController = rememberNavController()

    Scaffold { padding ->
        NavHost(navController, startDestination = "inicio", modifier = Modifier.padding(padding)) {

            // 1. Pantalla de Inicio
            composable("inicio") {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                    Text("WIM 100", style = MaterialTheme.typography.displayLarge)
                    Text("Gestión de Talentos", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(32.dp))

                    Button(onClick = { navController.navigate("postulacion") }, modifier = Modifier.fillMaxWidth()) {
                        Text("1. Postulante (Llenar CV)")
                    }
                    OutlinedButton(onClick = { navController.navigate("candidatas") }, modifier = Modifier.fillMaxWidth()) {
                        Text("2. Evaluadora (Ver y calificar)")
                    }

                    Spacer(Modifier.height(24.dp))

                    // BOTÓN PROTEGIDO CON HUELLA DIGITAL
                    FilledTonalButton(
                        onClick = {
                            BiometriaHelper.autenticar(
                                context = context,
                                alExito = { navController.navigate("direccion") },
                                alError = { error -> Toast.makeText(context, "Acceso denegado: $error", Toast.LENGTH_SHORT).show() }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("3. Dirección (Panel restringido)")
                    }
                }
            }

            // 2. Postulación
            composable("postulacion") {
                PostulacionScreen(viewModel = viewModel(factory = factory), onVolver = { navController.popBackStack() })
            }

            // 3. Bandeja de Candidatas
            composable("candidatas") {
                CandidatasScreen(
                    viewModel = viewModel(factory = factory),
                    onVolver = { navController.popBackStack() },
                    // Cuando aprietan el botón "Evaluar", viajamos a la ruta de evaluación pasándole el ID de la niña
                    onEvaluar = { id -> navController.navigate("evaluacion/$id") }
                )
            }

            // 4. Pantalla para poner las notas (Recibe un ID)
            composable("evaluacion/{candidataId}") { backStackEntry ->
                val idString = backStackEntry.arguments?.getString("candidataId")
                val id = idString?.toLongOrNull() ?: 0L

                EvaluacionScreen(
                    candidataId = id,
                    nombreEvaluadora = "Evaluadora", // Nombre de prueba
                    viewModel = viewModel(factory = factory),
                    onVolver = { navController.popBackStack() }
                )
            }

            // 5. Dirección (Ranking final)
            composable("direccion") {
                DireccionScreen(
                    nombreDirector = "Director",
                    viewModel = viewModel(factory = factory),
                    onVolver = { navController.popBackStack() }
                )
            }
        }
    }
}