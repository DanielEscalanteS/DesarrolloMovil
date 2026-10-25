
package com.example.wim100.ui.screens

import androidx.compose.foundation.layout.*
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

    LaunchedEffect(candidataId) {
        viewModel.cargar(candidataId)
    }

    // Toda la pantalla utiliza el componente reutilizable
    PantallaDesplazable {

        // Cabecera desplazable
        item(key = "cabecera") {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onVolver) {
                    Text("Volver")
                }

                Text(
                    text = "Detalle de evaluación",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        when {
            estado.cargando -> {
                item {
                    CircularProgressIndicator()
                }
            }

            estado.error != null -> {
                item {
                    Text(
                        text = estado.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                val ficha = estado.ficha

                if (ficha == null) {
                    item {
                        Text("No se encontró la evaluación.")
                    }
                } else {
                    val detalle = ficha.evaluaciones.singleOrNull()

                    val propia = detalle != null &&
                            nombreEvaluadora != null &&
                            detalle.evaluacion.evaluadoraClave ==
                            Evaluacion.normalizarClave(nombreEvaluadora)

                    // Información de la candidata
                    item(key = "candidata") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = ficha.candidata.nombre,
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Text(
                                    text = ficha.candidata.area,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Text(
                                    text = ficha.candidata.estado.etiqueta,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (
                                propia &&
                                !ficha.candidata.estado.estaResuelta
                            ) {
                                IconButton(
                                    onClick = { onEditar(candidataId) }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar evaluación"
                                    )
                                }
                            }
                        }
                    }

                    if (detalle == null) {
                        item {
                            Text(
                                "Esta candidata todavía no tiene una evaluación."
                            )
                        }
                    } else {

                        // Información de registro
                        item(key = "registro") {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val evaluacion = detalle.evaluacion

                                val formato =
                                    DateFormat.getDateTimeInstance(
                                        DateFormat.SHORT,
                                        DateFormat.SHORT
                                    )

                                Text(
                                    "Evaluadora: ${evaluacion.evaluadora}" +
                                            if (propia) " · Tu evaluación" else ""
                                )

                                Text(
                                    "Registrada: ${
                                        formato.format(Date(evaluacion.fecha))
                                    }",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                evaluacion.fechaEdicion?.let {
                                    Text(
                                        "Última edición: ${
                                            formato.format(Date(it))
                                        }",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        // Resumen de puntaje
                        item(key = "resumen") {
                            ResumenPuntaje(
                                detalle.evaluacion.puntajeTotal
                            )
                        }

                        // Fortalezas
                        item(key = "fortalezas") {
                            FortalezasEvaluacion(
                                estado.criterios,
                                detalle.puntajes
                            )
                        }

                        // Criterios y calificaciones
                        items(
                            items = estado.criterios,
                            key = { "criterio-${it.id}" }
                        ) { criterio ->

                            val puntaje = detalle.puntajes.find {
                                it.criterioId == criterio.id
                            }

                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement =
                                        Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Peso: ${criterio.peso}%",
                                        style = MaterialTheme.typography.labelMedium
                                    )

                                    BarraNota(
                                        criterio.nombre,
                                        puntaje?.puntaje,
                                        puntaje?.observacion.orEmpty()
                                    )
                                }
                            }
                        }

                        // Observación general
                        item(key = "observacion") {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Observación general",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        detalle.evaluacion.observacionGeneral
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
