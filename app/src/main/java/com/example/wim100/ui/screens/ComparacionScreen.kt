
package com.example.wim100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.ui.components.*
import com.example.wim100.viewmodel.ComparacionViewModel

@Composable
fun ComparacionScreen(
    ids: List<Long>,
    viewModel: ComparacionViewModel,
    onVerEvaluacion: (Long) -> Unit,
    onVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(ids) {
        viewModel.cargar(ids)
    }

    // Toda la pantalla utiliza el componente reutilizable.
    PantallaDesplazable {

        // Cabecera desplazable
        item(key = "cabecera") {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onVolver) {
                    Text("Volver y cambiar candidatas")
                }

                Text(
                    text = "Comparar candidatas",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        // Estados de la pantalla
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

            estado.candidatas.isEmpty() -> {
                item {
                    Text("No hay candidatas disponibles para comparar.")
                }
            }

            else -> {

                // Información general
                item(key = "informacion") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = estado.candidatas.first().candidata.area,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Compara sus fortalezas y justificaciones antes de decidir.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Tarjetas de candidatas
                items(
                    items = estado.candidatas,
                    key = { "candidata-${it.candidata.id}" }
                ) { ficha ->

                    val detalle = ficha.evaluaciones.singleOrNull()

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = ficha.candidata.nombre,
                                style = MaterialTheme.typography.titleLarge
                            )

                            if (detalle != null) {
                                Text(
                                    text = "${ficha.candidata.estado.etiqueta} · Evaluadora: ${detalle.evaluacion.evaluadora}",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                ResumenPuntaje(
                                    detalle.evaluacion.puntajeTotal
                                )

                                FortalezasEvaluacion(
                                    estado.criterios,
                                    detalle.puntajes
                                )

                                TextButton(
                                    onClick = {
                                        onVerEvaluacion(ficha.candidata.id)
                                    }
                                ) {
                                    Text("Ver evaluación completa")
                                }
                            } else {
                                Text("No hay una evaluación única disponible.")
                            }
                        }
                    }
                }

                item(key = "titulo-criterios") {
                    Text(
                        text = "Comparación por criterio",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                // Comparación de puntajes por criterio
                items(
                    items = estado.criterios,
                    key = { "criterio-${it.id}" }
                ) { criterio ->

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "${criterio.nombre} · ${criterio.peso}%",
                                style = MaterialTheme.typography.titleMedium
                            )

                            estado.candidatas.forEach { ficha ->

                                val nota = ficha.evaluaciones
                                    .singleOrNull()
                                    ?.puntajes
                                    ?.find {
                                        it.criterioId == criterio.id
                                    }

                                BarraNota(
                                    ficha.candidata.nombre,
                                    nota?.puntaje,
                                    nota?.observacion.orEmpty()
                                )

                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
