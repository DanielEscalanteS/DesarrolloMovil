package com.example.wim100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.model.EstadoCandidatura
import com.example.wim100.viewmodel.DireccionViewModel

@Composable
fun DireccionScreen(
    nombreDirector: String,
    viewModel: DireccionViewModel,
    onVolver: () -> Unit
) {
    // Escucha la lista en tiempo real desde el viewmodel
    val listaRanking by viewModel.rankingConsolidado.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Panel de Dirección - Ranking WIM 100", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        if (listaRanking.isEmpty()) {
            Text("Aún no hay candidatas en el sistema.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                // itemsIndexed da el número de la posición en el ranking
                itemsIndexed(listaRanking) { index, candidata ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("#${index + 1} - ${candidata.nombre}", style = MaterialTheme.typography.titleMedium)
                            Text("Área: ${candidata.area}", style = MaterialTheme.typography.bodySmall)

                            val promedio = candidata.promedio ?: 0.0
                            Text("Promedio: ${String.format("%.1f", promedio)} pts (${candidata.totalEvaluaciones} evaluaciones)", color = MaterialTheme.colorScheme.primary)
                            Text("Estado actual: ${candidata.estado.etiqueta}")

                            Spacer(modifier = Modifier.height(8.dp))

                            // Solo mostrar botones si la candidata fue "Evaluada" y no ha sido resuelta
                            if (candidata.estado == EstadoCandidatura.EVALUADA) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(onClick = { viewModel.resolverCandidatura(candidata.candidataId, EstadoCandidatura.NO_SELECCIONADA, nombreDirector) }) {
                                        Text("Rechazar")
                                    }
                                    Button(onClick = { viewModel.resolverCandidatura(candidata.candidataId, EstadoCandidatura.SELECCIONADA, nombreDirector) }) {
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