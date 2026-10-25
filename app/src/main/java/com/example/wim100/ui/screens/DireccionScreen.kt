
package com.example.wim100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.model.*
import com.example.wim100.ui.components.FiltroEstado
import com.example.wim100.ui.components.SelectorOpcion
import com.example.wim100.viewmodel.DireccionViewModel
import com.example.wim100.ui.components.PantallaDesplazable


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
    val listaRanking by viewModel.rankingConsolidado.collectAsStateWithLifecycle()
    val guardando by viewModel.guardando.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val cargando by viewModel.cargando.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val filtros by viewModel.filtros.collectAsStateWithLifecycle()
    val rankingFiltrado by viewModel.listado.collectAsStateWithLifecycle()
    val criterios by viewModel.criterios.collectAsStateWithLifecycle()
    val fichas by viewModel.fichas.collectAsStateWithLifecycle()
    val seleccionadas by viewModel.seleccionadas.collectAsStateWithLifecycle()

    var pendiente by remember {
        mutableStateOf<Pair<Long, EstadoCandidatura>?>(null)
    }

    var mostrarFiltros by rememberSaveable {
        mutableStateOf(false)
    }

    // Confirmación de selección o rechazo.
    pendiente?.let { resolucion ->
        AlertDialog(
            onDismissRequest = { pendiente = null },
            title = { Text("Confirmar resolución") },
            text = {
                Text(
                    "¿Marcar la candidatura N.º ${resolucion.first} como ${resolucion.second.etiqueta}?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resolverCandidatura(
                            resolucion.first,
                            resolucion.second,
                            sesion
                        )
                        pendiente = null
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendiente = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Toda la pantalla puede desplazarse verticalmente.
    PantallaDesplazable {

        // Cabecera y controles de orden.
        item(key = "cabecera") {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Panel de Dirección - Ranking WIM 100",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text("Responsable: $nombreDirector")

                OutlinedButton(onClick = onHistorial) {
                    Text("Ver historial de acciones")
                }

                mensaje?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                TextButton(
                    onClick = {
                        mostrarFiltros = !mostrarFiltros
                    }
                ) {
                    Text(
                        if (mostrarFiltros)
                            "Ocultar filtros"
                        else
                            "Filtros y orden"
                    )
                }

                Text(
                    text = "Orden: ${
                        criterios.find {
                            it.id == filtros.criterioId
                        }?.nombre ?: "Puntaje total"
                    } · ${
                        if (filtros.ascendente)
                            "Menor a mayor"
                        else
                            "Mayor a menor"
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Filtros desplazables junto al ranking.
        if (mostrarFiltros) {
            item(key = "filtros") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FiltroEstado(filtros.estado) {
                        viewModel.filtrar(
                            filtros.copy(estado = it)
                        )
                    }

                    SelectorOpcion(
                        "Área",
                        filtros.area,
                        listOf<String?>(null) + AREAS_ESPECIALIDAD,
                        { it ?: "Todas" },
                        {
                            viewModel.filtrar(
                                filtros.copy(area = it)
                            )
                        }
                    )

                    SelectorOpcion(
                        "Ordenar por",
                        filtros.criterioId,
                        listOf<Long?>(null) + criterios.map { it.id },
                        { id ->
                            criterios.find {
                                it.id == id
                            }?.nombre ?: "Puntaje total"
                        },
                        {
                            viewModel.filtrar(
                                filtros.copy(criterioId = it)
                            )
                        }
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !filtros.ascendente,
                            onClick = {
                                viewModel.filtrar(
                                    filtros.copy(ascendente = false)
                                )
                            },
                            label = { Text("Mayor a menor") }
                        )

                        FilterChip(
                            selected = filtros.ascendente,
                            onClick = {
                                viewModel.filtrar(
                                    filtros.copy(ascendente = true)
                                )
                            },
                            label = { Text("Menor a mayor") }
                        )
                    }
                }
            }
        }

        // Indicaciones y estado de carga.
        item(key = "indicaciones") {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Marca dos o tres candidatas evaluadas de la misma área.",
                    style = MaterialTheme.typography.bodySmall
                )

                when {
                    cargando -> CircularProgressIndicator()

                    error != null -> Text(
                        error.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )

                    listaRanking.isEmpty() ->
                        Text("Aún no hay candidatas en el sistema.")

                    rankingFiltrado.isEmpty() ->
                        Text("No hay candidatas con estos filtros.")
                }
            }
        }

        // Ranking de candidatas.
        if (!cargando && error == null) {
            itemsIndexed(
                items = rankingFiltrado,
                key = { _, candidata -> candidata.candidataId }
            ) { index, candidata ->

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${candidata.nombre}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium
                            )

                            Checkbox(
                                checked = candidata.candidataId in seleccionadas,
                                enabled = candidata.totalEvaluaciones == 1,
                                modifier = Modifier.semantics {
                                    contentDescription =
                                        "Comparar a ${candidata.nombre}"
                                },
                                onCheckedChange = {
                                    viewModel.alternarComparacion(
                                        candidata.candidataId
                                    )
                                }
                            )
                        }

                        Text(
                            "Área: ${candidata.area}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        val promedio = candidata.promedio

                        if (promedio == null) {
                            Text("Sin evaluaciones")
                        } else {
                            Text(
                                text = "Puntaje total: ${
                                    String.format("%.1f", promedio)
                                } / 100",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        filtros.criterioId?.let { id ->
                            val nota = fichas.find {
                                it.candidata.id == candidata.candidataId
                            }?.evaluaciones?.singleOrNull()
                                ?.puntajes?.find {
                                    it.criterioId == id
                                }?.puntaje

                            Text(
                                text = "${
                                    criterios.find { it.id == id }?.nombre
                                }: ${nota?.let { "$it / 5" } ?: "Sin nota"}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Text(
                            "Estado actual: ${candidata.estado.etiqueta}"
                        )

                        TextButton(
                            onClick = {
                                onVerEvaluaciones(candidata.candidataId)
                            }
                        ) {
                            Text("Ver evaluación")
                        }

                        candidata.motivoBloqueoResolucion?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // Acciones sobre la candidata evaluada.
                        if (
                            candidata.estado == EstadoCandidatura.EVALUADA &&
                            candidata.totalEvaluaciones >=
                            Evaluacion.MINIMO_PARA_CONSOLIDAR
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    enabled = !guardando,
                                    onClick = {
                                        pendiente = candidata.candidataId to
                                                EstadoCandidatura.NO_SELECCIONADA
                                    }
                                ) {
                                    Text("Rechazar")
                                }

                                Button(
                                    enabled = !guardando,
                                    onClick = {
                                        pendiente = candidata.candidataId to
                                                EstadoCandidatura.SELECCIONADA
                                    }
                                ) {
                                    Text("Seleccionar")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Acciones finales, también accesibles con scroll.
        item(key = "acciones") {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (seleccionadas.isNotEmpty()) {
                    Text(
                        text = "Selección: " + seleccionadas.mapNotNull { id ->
                            listaRanking.find {
                                it.candidataId == id
                            }?.nombre
                        }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        enabled = seleccionadas.size in 2..3,
                        onClick = { onComparar(seleccionadas) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Comparar (${seleccionadas.size})")
                    }

                    TextButton(
                        enabled = seleccionadas.isNotEmpty(),
                        onClick = viewModel::limpiarSeleccion
                    ) {
                        Text("Limpiar")
                    }
                }

                Button(
                    onClick = onVolver,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar Panel")
                }
            }
        }
    }
}

