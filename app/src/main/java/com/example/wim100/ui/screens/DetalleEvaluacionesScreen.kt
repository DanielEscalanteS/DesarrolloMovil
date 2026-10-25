package com.example.wim100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.data.local.EvaluacionConPuntajes
import com.example.wim100.model.CalculadoraPuntaje
import com.example.wim100.model.Criterio
import com.example.wim100.model.Evaluacion
import com.example.wim100.viewmodel.DetalleEvaluacionesViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun DetalleEvaluacionesScreen(
    candidataId: Long,
    nombreEvaluadora: String?,
    viewModel: DetalleEvaluacionesViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var comparar by rememberSaveable(candidataId) { mutableStateOf(false) }
    LaunchedEffect(candidataId) { viewModel.cargar(candidataId) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onVolver) { Text("Volver") }
        Text("Evaluaciones guardadas", style = MaterialTheme.typography.headlineSmall)
        when {
            estado.cargando -> CircularProgressIndicator()
            estado.error != null -> Text(estado.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            else -> estado.ficha?.let { ficha ->
                Text(ficha.candidata.nombre, style = MaterialTheme.typography.titleLarge)
                Text("${ficha.candidata.area} · ${ficha.candidata.estado.etiqueta}")
                Text("Consulta de notas y observaciones. Los datos guardados no se modifican aquí.", style = MaterialTheme.typography.bodySmall)
                if (ficha.evaluaciones.isEmpty()) {
                    Text("Esta candidata todavía no tiene evaluaciones.")
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !comparar, onClick = { comparar = false }, label = { Text("Por evaluadora") })
                        FilterChip(selected = comparar, onClick = { comparar = true }, label = { Text("Comparar notas") })
                    }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (comparar) {
                            item {
                                Text("Puntajes totales", style = MaterialTheme.typography.titleMedium)
                                ficha.evaluaciones.forEach { detalle ->
                                    Text("${detalle.evaluacion.evaluadora}: ${String.format("%.1f", detalle.evaluacion.puntajeTotal)} / 100")
                                }
                                if (ficha.evaluaciones.size < 2) Text("Se necesitan al menos dos evaluaciones para comparar.")
                            }
                            items(estado.criterios, key = { it.id }) { criterio ->
                                ElevatedCard(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("${criterio.nombre} (${criterio.peso}%)", style = MaterialTheme.typography.titleMedium)
                                        ficha.evaluaciones.forEach { detalle ->
                                            val puntaje = detalle.puntajes.find { it.criterioId == criterio.id }
                                            Text(detalle.evaluacion.evaluadora, color = MaterialTheme.colorScheme.primary)
                                            Text(puntaje?.let { "Nota: ${it.puntaje} / 5" } ?: "Sin nota guardada")
                                            if (!puntaje?.observacion.isNullOrBlank()) Text("Justificación: ${puntaje?.observacion}")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(ficha.evaluaciones, key = { it.evaluacion.id }) { detalle ->
                                EvaluacionGuardadaCard(detalle, estado.criterios, nombreEvaluadora)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EvaluacionGuardadaCard(detalle: EvaluacionConPuntajes, criterios: List<Criterio>, nombreEvaluadora: String?) {
    val evaluacion = detalle.evaluacion
    val propia = nombreEvaluadora != null && evaluacion.evaluadoraClave == Evaluacion.normalizarClave(nombreEvaluadora)
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(evaluacion.evaluadora, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            if (propia) Text("Tu evaluación", style = MaterialTheme.typography.labelLarge)
            Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(evaluacion.fecha)))
            Text("Puntaje total: ${String.format("%.1f", evaluacion.puntajeTotal)} / 100", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            criterios.forEach { criterio ->
                val puntaje = detalle.puntajes.find { it.criterioId == criterio.id }
                Text("${criterio.nombre} (${criterio.peso}%)", style = MaterialTheme.typography.titleSmall)
                Text(puntaje?.let { "Nota: ${it.puntaje} / 5 · ${CalculadoraPuntaje.nivel(it.puntaje)}" } ?: "Sin nota guardada")
                Text("Justificación: ${puntaje?.observacion?.takeIf { it.isNotBlank() } ?: "Sin observación adicional"}")
            }
            HorizontalDivider()
            Text("Observación general", style = MaterialTheme.typography.titleSmall)
            Text(evaluacion.observacionGeneral)
        }
    }
}
