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
import com.example.wim100.ui.components.SelectorOpcion
import com.example.wim100.model.AREAS_ESPECIALIDAD
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun DireccionScreen(
    nombreDirector: String,
    viewModel: DireccionViewModel,
    sesion: Sesion,
    onHistorial: () -> Unit,
    onVerEvaluaciones: (Long) -> Unit,
    onComparar: (List<Long>) -> Unit,
    onVolver: () -> Unit
) {
    // Escucha la lista en tiempo real desde el viewmodel
    val listaRanking by viewModel.rankingConsolidado.collectAsStateWithLifecycle()

    val guardando by viewModel.guardando.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val cargando by viewModel.cargando.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var pendiente by remember { mutableStateOf<Pair<Long, EstadoCandidatura>?>(null) }
    val filtros by viewModel.filtros.collectAsStateWithLifecycle()
    val rankingFiltrado by viewModel.listado.collectAsStateWithLifecycle()
    val criterios by viewModel.criterios.collectAsStateWithLifecycle()
    val fichas by viewModel.fichas.collectAsStateWithLifecycle()
    val seleccionadas by viewModel.seleccionadas.collectAsStateWithLifecycle()
    var mostrarFiltros by rememberSaveable { mutableStateOf(false) }

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
        TextButton(onClick = { mostrarFiltros = !mostrarFiltros }) { Text(if (mostrarFiltros) "Ocultar filtros" else "Filtros y orden") }
        Text("Orden: ${criterios.find { it.id == filtros.criterioId }?.nombre ?: "Puntaje total"} · ${if (filtros.ascendente) "Menor a mayor" else "Mayor a menor"}", style = MaterialTheme.typography.bodySmall)

        if (cargando) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
        } else if (listaRanking.isEmpty()) {
            Text("Aún no hay candidatas en el sistema.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    if (mostrarFiltros) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FiltroEstado(filtros.estado) { viewModel.filtrar(filtros.copy(estado = it)) }
                            SelectorOpcion("Área", filtros.area, listOf<String?>(null) + AREAS_ESPECIALIDAD,
                                { it ?: "Todas" }, { viewModel.filtrar(filtros.copy(area = it)) })
                            SelectorOpcion("Ordenar por", filtros.criterioId, listOf<Long?>(null) + criterios.map { it.id },
                                { id -> criterios.find { it.id == id }?.nombre ?: "Puntaje total" },
                                { viewModel.filtrar(filtros.copy(criterioId = it)) })
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = !filtros.ascendente, onClick = { viewModel.filtrar(filtros.copy(ascendente = false)) }, label = { Text("Mayor a menor") })
                                FilterChip(selected = filtros.ascendente, onClick = { viewModel.filtrar(filtros.copy(ascendente = true)) }, label = { Text("Menor a mayor") })
                            }
                        }
                    }
                    Text("Marca dos o tres candidatas evaluadas de la misma área.", style = MaterialTheme.typography.bodySmall)
                    if (rankingFiltrado.isEmpty()) Text("No hay candidatas con estos filtros.")
                }
                // itemsIndexed da el número de la posición en el ranking
                itemsIndexed(rankingFiltrado, key = { _, candidata -> candidata.candidataId }) { index, candidata ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}. ${candidata.nombre}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                Checkbox(checked = candidata.candidataId in seleccionadas, enabled = candidata.totalEvaluaciones == 1,
                                    modifier = Modifier.semantics { contentDescription = "Comparar a ${candidata.nombre}" },
                                    onCheckedChange = { viewModel.alternarComparacion(candidata.candidataId) })
                            }
                            Text("Área: ${candidata.area}", style = MaterialTheme.typography.bodySmall)

                            val promedio = candidata.promedio ?: 0.0
                            if (candidata.promedio == null) Text("Sin evaluaciones")
                            if (candidata.promedio != null) Text("Puntaje total: ${String.format("%.1f", promedio)} / 100", color = MaterialTheme.colorScheme.primary)
                            filtros.criterioId?.let { id ->
                                val nota = fichas.find { it.candidata.id == candidata.candidataId }?.evaluaciones?.singleOrNull()?.puntajes?.find { it.criterioId == id }?.puntaje
                                Text("${criterios.find { it.id == id }?.nombre}: ${nota?.let { "$it / 5" } ?: "Sin nota"}", style = MaterialTheme.typography.labelLarge)
                            }
                            Text("Estado actual: ${candidata.estado.etiqueta}")
                            TextButton(onClick = { onVerEvaluaciones(candidata.candidataId) }) { Text("Ver evaluación") }
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
        if (seleccionadas.isNotEmpty()) {
            Text("Selección: " + seleccionadas.mapNotNull { id -> listaRanking.find { it.candidataId == id }?.nombre }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(enabled = seleccionadas.size in 2..3, onClick = { onComparar(seleccionadas) }, modifier = Modifier.weight(1f)) { Text("Comparar (${seleccionadas.size})") }
            TextButton(enabled = seleccionadas.isNotEmpty(), onClick = viewModel::limpiarSeleccion) { Text("Limpiar") }
        }
        Button(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar Panel")
        }
    }
}
