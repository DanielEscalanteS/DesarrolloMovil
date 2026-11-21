package com.example.wim100.navigation

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.wim100.model.Rol
import com.example.wim100.model.Sesion
import com.example.wim100.ui.screens.*
import com.example.wim100.viewmodel.*

@Composable
fun AccesoNavHost(factory: AppViewModelFactory, sesionViewModel: SesionViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Rutas.INICIO) {
        composable(Rutas.INICIO) {
            InicioScreen(
                onPostular = { navController.navigate(Rutas.CANDIDATA_NUEVA) },
                onIngresar = { navController.navigate(Rutas.LOGIN) })
        }
        composable(Rutas.LOGIN) {
            val estado by sesionViewModel.loginState.collectAsStateWithLifecycle()
            val volver = {
                if (!estado.cargando) {
                    sesionViewModel.cerrar()
                    navController.popBackStack()
                }
                Unit
            }
            BackHandler(onBack = volver)
            LoginScreen(sesionViewModel, onVolver = volver)
        }
        composable(Rutas.CANDIDATA_NUEVA) {
            PostulacionScreen(viewModel = viewModel(factory = factory),
                onVolver = { navController.popBackStack() })
        }
    }
}

@Composable
fun WimNavHost(sesion: Sesion, factory: AppViewModelFactory, onCerrar: () -> Unit) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var salir by remember { mutableStateOf(false) }
    val entrada by navController.currentBackStackEntryAsState()
    val inicio = if (sesion.rol == Rol.EVALUADORA) Rutas.CANDIDATAS else Rutas.DIRECCION

    BackHandler(entrada?.destination?.route == inicio) { salir = true }
    if (salir) {
        AlertDialog(onDismissRequest = { salir = false },
            title = { Text("Cerrar sesión") },
            text = { Text("Los cambios sin enviar no se guardarán.") },
            confirmButton = { TextButton(onClick = { salir = false; onCerrar() }) { Text("Salir") } },
            dismissButton = { TextButton(onClick = { salir = false }) { Text("Continuar") } })
    }

    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = { salir = true }) { Text("Cerrar sesión (${sesion.rol.etiqueta})") }
        NavHost(navController, startDestination = inicio, modifier = Modifier.weight(1f)) {
            composable(Rutas.CANDIDATAS) {
                if (sesion.rol == Rol.EVALUADORA) {
                    CandidatasScreen(
                        viewModel = viewModel(factory = factory),
                        nombreEvaluadora = sesion.nombre,
                        onVolver = { salir = true },
                        onCrear = { navController.navigate(Rutas.CANDIDATA_NUEVA) },
                        onVerEvaluaciones = { navController.navigate(Rutas.detalleEvaluaciones(it)) },
                        onEvaluar = { navController.navigate(Rutas.evaluacion(it)) },
                        onVerPostulacion = { navController.navigate(Rutas.detalleCandidata(it)) })
                } else Text("Acceso no permitido")
            }

            composable(Rutas.CANDIDATA_NUEVA) {
                if (sesion.rol == Rol.EVALUADORA) {
                    PostulacionScreen(viewModel = viewModel(factory = factory), usuario = sesion.nombre,
                        onVolver = { navController.popBackStack() })
                } else Text("Acceso no permitido")
            }

            composable(Rutas.CANDIDATA_EDITAR) { entradaFormulario ->
                val id = entradaFormulario.arguments?.getString("candidataId")?.toLongOrNull()
                if (sesion.rol == Rol.EVALUADORA && id != null && id > 0) {
                    PostulacionScreen(viewModel = viewModel(factory = factory), usuario = sesion.nombre,
                        candidataId = id, onVolver = { navController.popBackStack() })
                } else Text("Acceso no permitido")
            }

            composable(Rutas.CANDIDATA_DETALLE) { entradaDetalle ->
                val id = entradaDetalle.arguments?.getString("candidataId")?.toLongOrNull()
                if (id != null && id > 0) {
                    val detalle: DetalleCandidataViewModel = viewModel(factory = factory)
                    val estado by detalle.uiState.collectAsStateWithLifecycle()
                    LaunchedEffect(id) { detalle.cargar(id) }
                    LaunchedEffect(estado.eliminada) {
                        if (estado.eliminada) {
                            Toast.makeText(context, estado.error ?: "Candidatura eliminada", Toast.LENGTH_LONG).show()
                            navController.popBackStack()
                        }
                    }
                    val candidata = estado.candidata
                    when {
                        estado.cargando -> CircularProgressIndicator()
                        candidata == null -> Column {
                            Text(estado.error ?: "La candidata ya no está disponible")
                            TextButton(onClick = { navController.popBackStack() }) { Text("Volver") }
                        }
                        else -> DetallePostulacionScreen(
                            candidata = candidata, rol = sesion.rol,
                            eliminando = estado.eliminando, error = estado.error,
                            onVolver = { navController.popBackStack() },
                            onEditar = { navController.navigate(Rutas.editarCandidata(id)) },
                            onEliminar = { detalle.eliminar(sesion) })
                    }
                } else Text("Identificador de candidatura inválido")
            }

            composable(Rutas.EVALUACION) { entradaEvaluacion ->
                val id = entradaEvaluacion.arguments?.getString("candidataId")?.toLongOrNull()
                if (sesion.rol == Rol.EVALUADORA && id != null && id > 0) {
                    EvaluacionScreen(candidataId = id, sesion = sesion,
                        viewModel = viewModel(factory = factory),
                        onVolver = { navController.popBackStack() })
                } else Text("Acceso no permitido")
            }

            composable(Rutas.DIRECCION) {
                if (sesion.rol == Rol.DIRECCION) {
                    DireccionScreen(nombreDirector = sesion.nombre, sesion = sesion,
                        viewModel = viewModel(factory = factory),
                        onHistorial = { navController.navigate(Rutas.HISTORIAL) },
                        onVerEvaluaciones = { navController.navigate(Rutas.detalleEvaluaciones(it)) },
                        onVerPostulacion = { navController.navigate(Rutas.detalleCandidata(it)) },
                        onComparar = { navController.navigate(Rutas.comparar(it)) },
                        onVolver = { salir = true })
                } else Text("Acceso no permitido")
            }

            composable(Rutas.HISTORIAL) {
                if (sesion.rol == Rol.DIRECCION) {
                    HistorialScreen(viewModel = viewModel(factory = factory),
                        onVolver = { navController.popBackStack() })
                } else Text("Acceso no permitido")
            }

            composable(Rutas.DETALLE_EVALUACIONES) { entradaEvaluaciones ->
                val id = entradaEvaluaciones.arguments?.getString("candidataId")?.toLongOrNull()
                if (id != null && id > 0) {
                    DetalleEvaluacionesScreen(candidataId = id,
                        nombreEvaluadora = sesion.nombre.takeIf { sesion.rol == Rol.EVALUADORA },
                        viewModel = viewModel(factory = factory),
                        onEditar = { navController.navigate(Rutas.evaluacion(it)) },
                        onVolver = { navController.popBackStack() })
                } else Text("Identificador de candidatura inválido")
            }

            composable(Rutas.COMPARAR) { entradaComparacion ->
                if (sesion.rol == Rol.DIRECCION) {
                    val ids = entradaComparacion.arguments?.getString("ids").orEmpty()
                        .split(",").mapNotNull { it.toLongOrNull() }
                    ComparacionScreen(ids = ids, viewModel = viewModel(factory = factory),
                        onVerEvaluacion = { navController.navigate(Rutas.detalleEvaluaciones(it)) },
                        onVolver = { navController.popBackStack() })
                } else Text("Acceso no permitido")
            }
        }
    }
}
