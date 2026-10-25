package com.example.wim100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.ui.components.CampoFormulario
import com.example.wim100.viewmodel.EvaluacionViewModel
import com.example.wim100.model.Sesion

@Composable
fun EvaluacionScreen(
    candidataId: Long,
    sesion: Sesion,
    viewModel: EvaluacionViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(candidataId) {
        viewModel.cargarCriterios(candidataId, sesion)
    }

    if (estado.cargando || !estado.puedeGuardar) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (estado.cargando) CircularProgressIndicator()
            else Text(estado.errorGeneral ?: "No se puede editar esta evaluación", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onVolver) { Text("Volver") }
        }
        return
    }

    if (estado.finalizadoConExito) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(if (estado.editando) "Cambios guardados" else "Evaluación Exitosa") },
            text = { Text(if (estado.editando) "La evaluación fue actualizada y la modificación quedó en el historial." else "Los puntajes han sido guardados en el registro histórico.") },
            confirmButton = { TextButton(onClick = onVolver) { Text("Aceptar") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Cabecera
        Text("Matriz WIM 100", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(estado.candidataNombre, style = MaterialTheme.typography.titleMedium)
        Text(if (estado.editando) "Editando tu evaluación" else "Evaluadora: ${sesion.nombre}")
        Text(
            text = "Nota ponderada: ${String.format("%.1f", estado.puntajeTotal)} / 100",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(estado.criterios) { criterio ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(criterio.nombre, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(criterio.descripcion, style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Asignar nota (1 al 5):", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Fila de botones
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (nota in 1..5) {
                                val seleccionado = estado.puntajes[criterio.id] == nota

                                if (seleccionado) {
                                    Button(
                                        onClick = { viewModel.actualizarPuntaje(criterio.id, nota) },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text(nota.toString()) }
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.actualizarPuntaje(criterio.id, nota) },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text(nota.toString()) }
                                }
                            }
                        }

                        // Solo se muestra el campo si la nota es 1 o 2
                        val notaActual = estado.puntajes[criterio.id]

                        if (notaActual == 1 || notaActual == 2 || estado.observaciones[criterio.id].orEmpty().isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val textoObservacion = estado.observaciones[criterio.id] ?: ""
                            CampoFormulario(
                                valor = textoObservacion,
                                etiqueta = if (notaActual == 1 || notaActual == 2) "Justificación (obligatorio con nota 1 o 2)" else "Observación del criterio",
                                error = null,
                                onCambio = { viewModel.actualizarObservacion(criterio.id, it) }
                            )
                        }
                    }
                }
            }

            // Botones Finales
            item {
                Spacer(modifier = Modifier.height(8.dp))
                CampoFormulario(
                    valor = estado.observacionGeneral,
                    etiqueta = "Observación general (mín. 20 caracteres)",
                    error = estado.errorGeneral,
                    onCambio = { viewModel.actualizarObservacionGeneral(it) },
                    multilinea = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.guardarEvaluacion(candidataId, sesion) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !estado.guardando
                ) {
                    Text(if (estado.guardando) "Guardando en Base de Datos..." else if (estado.editando) "Guardar cambios" else "Registrar Evaluación", style = MaterialTheme.typography.titleMedium)
                }

                TextButton(onClick = onVolver, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Cancelar y volver")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
