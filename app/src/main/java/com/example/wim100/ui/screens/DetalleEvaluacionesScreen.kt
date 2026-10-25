package com.example.wim100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.model.Evaluacion
import com.example.wim100.ui.components.*
import com.example.wim100.viewmodel.DetalleEvaluacionesViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun DetalleEvaluacionesScreen(
    candidataId: Long,
    nombreEvaluadora: String?,
    viewModel: DetalleEvaluacionesViewModel,
    onEditar: (Long) -> Unit,
    onVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(candidataId) { viewModel.cargar(candidataId) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onVolver) { Text("Volver") }
        Text("Detalle de evaluación", style = MaterialTheme.typography.headlineSmall)
        when {
            estado.cargando -> CircularProgressIndicator()
            estado.error != null -> Text(estado.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            else -> estado.ficha?.let { ficha ->
                val detalle = ficha.evaluaciones.singleOrNull()
                val propia = detalle != null && nombreEvaluadora != null &&
                    detalle.evaluacion.evaluadoraClave == Evaluacion.normalizarClave(nombreEvaluadora)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ficha.candidata.nombre, style = MaterialTheme.typography.titleLarge)
                        Text(ficha.candidata.area, style = MaterialTheme.typography.bodyMedium)
                        Text(ficha.candidata.estado.etiqueta, color = MaterialTheme.colorScheme.primary)
                    }
                    if (propia && !ficha.candidata.estado.estaResuelta) {
                        IconButton(onClick = { onEditar(candidataId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar evaluación")
                        }
                    }
                }
                if (detalle == null) Text("Esta candidata todavía no tiene una evaluación.")
                else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        val evaluacion = detalle.evaluacion
                        val formato = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        Text("Evaluadora: ${evaluacion.evaluadora}" + if (propia) " · Tu evaluación" else "")
                        Text("Registrada: ${formato.format(Date(evaluacion.fecha))}", style = MaterialTheme.typography.bodySmall)
                        evaluacion.fechaEdicion?.let { Text("Última edición: ${formato.format(Date(it))}", style = MaterialTheme.typography.bodySmall) }
                    }
                    item { ResumenPuntaje(detalle.evaluacion.puntajeTotal) }
                    item { FortalezasEvaluacion(estado.criterios, detalle.puntajes) }
                    items(estado.criterios, key = { it.id }) { criterio ->
                        val puntaje = detalle.puntajes.find { it.criterioId == criterio.id }
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Peso: ${criterio.peso}%", style = MaterialTheme.typography.labelMedium)
                                BarraNota(criterio.nombre, puntaje?.puntaje, puntaje?.observacion.orEmpty())
                            }
                        }
                    }
                    item {
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Observación general", style = MaterialTheme.typography.titleMedium)
                                Text(detalle.evaluacion.observacionGeneral)
                            }
                        }
                    }
                }
            }
        }
    }
}
