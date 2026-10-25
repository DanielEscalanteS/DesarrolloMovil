package com.example.wim100.ui

import android.widget.Toast
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.wim100.model.Rol
import com.example.wim100.viewmodel.SesionViewModel
import com.example.wim100.ui.screens.HistorialScreen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
            evaluacionRepository = application.container.evaluacionRepository,
            historialRepository = application.container.historialRepository
        )
    }

    val navController = rememberNavController()

    val sesionViewModel: SesionViewModel = viewModel(factory = factory)
    val sesion by sesionViewModel.sesion.collectAsStateWithLifecycle()
    val errorSesion by sesionViewModel.error.collectAsStateWithLifecycle()
    var nombre by remember { mutableStateOf("") }
    var verificando by remember { mutableStateOf(false) }
    var salir by remember { mutableStateOf(false) }
    val entrada by navController.currentBackStackEntryAsState()

    LaunchedEffect(sesion) {
        val actual = sesion
        if (actual == null) {
            navController.popBackStack("inicio", false)
        } else {
            val destino = when (actual.rol) {
                Rol.POSTULANTE -> "postulacion"
                Rol.EVALUADORA -> "candidatas"
                Rol.DIRECCION -> "direccion"
            }
            navController.navigate(destino) { launchSingleTop = true }
        }
    }
    BackHandler(sesion != null && entrada?.destination?.route in listOf("postulacion", "candidatas", "direccion")) {
        salir = true
    }
    if (salir) {
        AlertDialog(
            onDismissRequest = { salir = false },
            title = { Text("Cerrar sesión") },
            text = { Text("Los cambios sin enviar no se guardarán.") },
            confirmButton = { TextButton(onClick = { sesionViewModel.cerrar(); salir = false }) { Text("Salir") } },
            dismissButton = { TextButton(onClick = { salir = false }) { Text("Continuar") } }
        )
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (sesion != null) {
                TextButton(onClick = { salir = true }) { Text("Cerrar sesión (${sesion?.rol?.etiqueta})") }
            }
        NavHost(navController, startDestination = "inicio", modifier = Modifier.weight(1f)) {

            // 1. Pantalla de Inicio
            composable("inicio") {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
                    Text("WIM 100", style = MaterialTheme.typography.displayLarge)
                    Text("Gestión de Talentos", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(32.dp))

                    Text("Acceso de demostración")
                    OutlinedTextField(value = nombre, onValueChange = { nombre = it },
                        label = { Text("Nombre de prueba") }, enabled = !verificando, modifier = Modifier.fillMaxWidth())
                    errorSesion?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                    Button(enabled = !verificando, onClick = { sesionViewModel.iniciar(nombre, Rol.POSTULANTE) }, modifier = Modifier.fillMaxWidth()) {
                        Text("1. Postulante (Llenar CV)")
                    }
                    OutlinedButton(enabled = !verificando, onClick = { sesionViewModel.iniciar(nombre, Rol.EVALUADORA) }, modifier = Modifier.fillMaxWidth()) {
                        Text("2. Evaluadora (Ver y calificar)")
                    }

                    Spacer(Modifier.height(24.dp))

                    // BOTÓN PROTEGIDO CON HUELLA DIGITAL
                    FilledTonalButton(
                        enabled = !verificando,
                        onClick = {
                            if (nombre.trim().length < 3) {
                                Toast.makeText(context, "Ingresa un nombre de al menos 3 caracteres", Toast.LENGTH_SHORT).show()
                            } else {
                            verificando = true
                            BiometriaHelper.autenticar(
                                context = context,
                                alExito = { verificando = false; sesionViewModel.iniciar(nombre, Rol.DIRECCION, true) },
                                alError = { error -> verificando = false; Toast.makeText(context, "Acceso denegado: $error", Toast.LENGTH_SHORT).show() }
                            )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("3. Dirección (Panel restringido)")
                    }
                    TextButton(onClick = {
                        try { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
                        catch (e: Exception) { Toast.makeText(context, "Abre Seguridad en los ajustes", Toast.LENGTH_SHORT).show() }
                    }) { Text("Configurar seguridad del dispositivo") }
                }
            }

            // 2. Postulación
            composable("postulacion") {
                if (sesion?.rol == Rol.POSTULANTE) {
                PostulacionScreen(viewModel = viewModel(factory = factory), onVolver = { salir = true })
                } else Text("Acceso no permitido")
            }

            // 3. Bandeja de Candidatas
            composable("candidatas") {
                if (sesion?.rol == Rol.EVALUADORA) {
                CandidatasScreen(
                    viewModel = viewModel(factory = factory),
                    onVolver = { salir = true },
                    // Cuando aprietan el botón "Evaluar", viajamos a la ruta de evaluación pasándole el ID de la niña
                    onEvaluar = { id -> navController.navigate("evaluacion/$id") }
                )
                } else Text("Acceso no permitido")
            }

            // 4. Pantalla para poner las notas (Recibe un ID)
            composable("evaluacion/{candidataId}") { backStackEntry ->
                val idString = backStackEntry.arguments?.getString("candidataId")
                val id = idString?.toLongOrNull() ?: 0L

                if (sesion?.rol == Rol.EVALUADORA && id > 0) {
                EvaluacionScreen(
                    candidataId = id,
                    nombreEvaluadora = checkNotNull(sesion).nombre,
                    viewModel = viewModel(factory = factory),
                    onVolver = { navController.popBackStack() }
                )
                } else Text("Acceso no permitido")
            }

            // 5. Dirección (Ranking final)
            composable("direccion") {
                if (sesion?.rol == Rol.DIRECCION) {
                DireccionScreen(
                    nombreDirector = checkNotNull(sesion).nombre,
                    sesion = checkNotNull(sesion),
                    onHistorial = { navController.navigate("historial") },
                    viewModel = viewModel(factory = factory),
                    onVolver = { salir = true }
                )
                } else Text("Acceso no permitido")
            }

            composable("historial") {
                if (sesion?.rol == Rol.DIRECCION) {
                    HistorialScreen(viewModel = viewModel(factory = factory), onVolver = { navController.popBackStack() })
                } else Text("Acceso no permitido")
            }
        }
        }
    }
}