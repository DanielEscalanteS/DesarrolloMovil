package com.example.wim100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.model.EstadoCandidatura
import com.example.wim100.model.Sesion
import com.example.wim100.model.Evaluacion
import com.example.wim100.viewmodel.DireccionViewModel
import com.example.wim100.ui.components.FiltroEstado

@Composable
fun DireccionScreen(
    nombreDirector: String,
    viewModel: DireccionViewModel,
    sesion: Sesion,
    onHistorial: () -> Unit,
    onVerEvaluaciones: (Long) -> Unit,
    onVolver: () -> Unit
) {
    // Escucha la lista en tiempo real desde el viewmodel
    val listaRanking by viewModel.rankingConsolidado.collectAsStateWithLifecycle()

    val guardando by viewModel.guardando.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val cargando by viewModel.cargando.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var pendiente by remember { mutableStateOf<Pair<Long, EstadoCandidatura>?>(null) }
    var filtro by rememberSaveable { mutableStateOf<EstadoCandidatura?>(null) }
    val rankingFiltrado = listaRanking.filter { filtro == null || it.estado == filtro }

    pendiente?.let { resolucion ->
        AlertDialog(
            onDismissRequest = { pendiente = null },
            title = { Text("Confirmar resolución") },
            text = { Text("¿Marcar la candidatura N.º ${resolucion.first} como ${resolucion.second.etiqueta}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resolverCandidatura(resolucion.first, resolucion.second, sesion)
                    pendiente = null
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { pendiente = null }) { Text("Cancelar") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Panel de Dirección - Ranking WIM 100", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Responsable: $nombreDirector")
        OutlinedButton(onClick = onHistorial) { Text("Ver historial de acciones") }
        mensaje?.let { Text(it) }
        FiltroEstado(filtro) { filtro = it }

        if (cargando) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
        } else if (listaRanking.isEmpty()) {
            Text("Aún no hay candidatas en el sistema.")
        } else {
            if (rankingFiltrado.isEmpty()) Text("No hay candidatas con este estado.")
            LazyColumn(modifier = Modifier.weight(1f)) {
                // itemsIndexed da el número de la posición en el ranking
                itemsIndexed(rankingFiltrado, key = { _, candidata -> candidata.candidataId }) { _, candidata ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val posicion = listaRanking.indexOfFirst { it.candidataId == candidata.candidataId } + 1
                            Text("#$posicion - ${candidata.nombre}", style = MaterialTheme.typography.titleMedium)
                            Text("Área: ${candidata.area}", style = MaterialTheme.typography.bodySmall)

                            val promedio = candidata.promedio ?: 0.0
                            if (candidata.promedio == null) Text("Sin evaluaciones")
                            Text("Promedio: ${String.format("%.1f", promedio)} pts (${candidata.totalEvaluaciones} evaluaciones)", color = MaterialTheme.colorScheme.primary)
                            Text("Estado actual: ${candidata.estado.etiqueta}")
                            TextButton(onClick = { onVerEvaluaciones(candidata.candidataId) }) { Text("Ver evaluaciones y comparar") }
                            candidata.motivoBloqueoResolucion?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Solo mostrar botones si la candidata fue "Evaluada" y no ha sido resuelta
                            if (candidata.estado == EstadoCandidatura.EVALUADA && candidata.totalEvaluaciones >= Evaluacion.MINIMO_PARA_CONSOLIDAR) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(enabled = !guardando, onClick = { pendiente = candidata.candidataId to EstadoCandidatura.NO_SELECCIONADA }) {
                                        Text("Rechazar")
                                    }
                                    Button(enabled = !guardando, onClick = { pendiente = candidata.candidataId to EstadoCandidatura.SELECCIONADA }) {
                                        Text("Seleccionar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar Panel")
        }
    }
}
