
package com.example.wim100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.model.Sesion
import com.example.wim100.ui.components.CampoFormulario
import com.example.wim100.ui.components.PantallaDesplazable
import com.example.wim100.viewmodel.EvaluacionViewModel

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

    // Pantalla de carga o acceso restringido
    if (estado.cargando || !estado.puedeGuardar) {
        PantallaDesplazable {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (estado.cargando) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            estado.errorGeneral
                                ?: "No se puede editar esta evaluación",
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    TextButton(onClick = onVolver) {
                        Text("Volver")
                    }
                }
            }
        }
        return
    }

    // Confirmación de evaluación
    if (estado.finalizadoConExito) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    if (estado.editando)
                        "Cambios guardados"
                    else
                        "Evaluación Exitosa"
                )
            },
            text = {
                Text(
                    if (estado.editando)
                        "La evaluación fue actualizada y la modificación quedó en el historial."
                    else
                        "Los puntajes han sido guardados en el registro histórico."
                )
            },
            confirmButton = {
                TextButton(onClick = onVolver) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Pantalla principal con desplazamiento completo
    PantallaDesplazable {

        // Cabecera desplazable
        item(key = "cabecera") {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Matriz WIM 100",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = estado.candidataNombre,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    if (estado.editando)
                        "Editando tu evaluación"
                    else
                        "Evaluadora: ${sesion.nombre}"
                )

                Text(
                    text = "Nota ponderada: ${
                        String.format("%.1f", estado.puntajeTotal)
                    } / 100",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Criterios de evaluación
        items(
            items = estado.criterios,
            key = { it.id }
        ) { criterio ->

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme
                        .surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = criterio.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = criterio.descripcion,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Asignar nota (1 al 5):",
                        style = MaterialTheme.typography.labelLarge
                    )

                    // Botones de notas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (nota in 1..5) {
                            val seleccionado =
                                estado.puntajes[criterio.id] == nota

                            if (seleccionado) {
                                Button(
                                    onClick = {
                                        viewModel.actualizarPuntaje(
                                            criterio.id, nota
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(nota.toString())
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.actualizarPuntaje(
                                            criterio.id, nota
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(nota.toString())
                                }
                            }
                        }
                    }

                    // Justificación para notas bajas
                    val notaActual = estado.puntajes[criterio.id]

                    if (
                        notaActual == 1 ||
                        notaActual == 2 ||
                        estado.observaciones[criterio.id]
                            .orEmpty().isNotBlank()
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        val textoObservacion =
                            estado.observaciones[criterio.id] ?: ""

                        CampoFormulario(
                            valor = textoObservacion,
                            etiqueta = if (
                                notaActual == 1 || notaActual == 2
                            ) {
                                "Justificación (obligatorio con nota 1 o 2)"
                            } else {
                                "Observación del criterio"
                            },
                            error = null,
                            onCambio = {
                                viewModel.actualizarObservacion(
                                    criterio.id, it
                                )
                            }
                        )
                    }
                }
            }
        }

        // Observación general y botones de guardado
        item(key = "acciones") {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CampoFormulario(
                    valor = estado.observacionGeneral,
                    etiqueta = "Observación general (mín. 20 caracteres)",
                    error = estado.errorGeneral,
                    onCambio = {
                        viewModel.actualizarObservacionGeneral(it)
                    },
                    multilinea = true
                )

                Button(
                    onClick = {
                        viewModel.guardarEvaluacion(
                            candidataId, sesion
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !estado.guardando
                ) {
                    Text(
                        text = when {
                            estado.guardando ->
                                "Guardando en Base de Datos..."
                            estado.editando ->
                                "Guardar cambios"
                            else ->
                                "Registrar Evaluación"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                TextButton(
                    onClick = onVolver,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar y volver")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
