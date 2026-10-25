package com.example.wim100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.ui.components.*
import com.example.wim100.viewmodel.ComparacionViewModel

@Composable
fun ComparacionScreen(ids: List<Long>, viewModel: ComparacionViewModel, onVerEvaluacion: (Long) -> Unit, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(ids) { viewModel.cargar(ids) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onVolver) { Text("Volver y cambiar candidatas") }
        Text("Comparar candidatas", style = MaterialTheme.typography.headlineSmall)
        when {
            estado.cargando -> CircularProgressIndicator()
            estado.error != null -> Text(estado.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            else -> {
                Text(estado.candidatas.first().candidata.area, style = MaterialTheme.typography.titleMedium)
                Text("Compara sus fortalezas y justificaciones antes de decidir.", style = MaterialTheme.typography.bodySmall)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(estado.candidatas, key = { "candidata-${it.candidata.id}" }) { ficha ->
                        val detalle = ficha.evaluaciones.single()
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(ficha.candidata.nombre, style = MaterialTheme.typography.titleLarge)
                                Text("${ficha.candidata.estado.etiqueta} · Evaluadora: ${detalle.evaluacion.evaluadora}", style = MaterialTheme.typography.bodySmall)
                                ResumenPuntaje(detalle.evaluacion.puntajeTotal)
                                FortalezasEvaluacion(estado.criterios, detalle.puntajes)
                                TextButton(onClick = { onVerEvaluacion(ficha.candidata.id) }) { Text("Ver evaluación completa") }
                            }
                        }
                    }
                    item { Text("Comparación por criterio", style = MaterialTheme.typography.titleLarge) }
                    items(estado.criterios, key = { "criterio-${it.id}" }) { criterio ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("${criterio.nombre} · ${criterio.peso}%", style = MaterialTheme.typography.titleMedium)
                                estado.candidatas.forEach { ficha ->
                                    val nota = ficha.evaluaciones.single().puntajes.find { it.criterioId == criterio.id }
                                    BarraNota(ficha.candidata.nombre, nota?.puntaje, nota?.observacion.orEmpty())
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
