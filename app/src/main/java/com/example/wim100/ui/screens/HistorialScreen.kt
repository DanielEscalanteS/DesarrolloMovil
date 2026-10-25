
package com.example.wim100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.ui.components.PantallaDesplazable
import com.example.wim100.viewmodel.HistorialViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun HistorialScreen(
    viewModel: HistorialViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    PantallaDesplazable {

        // Cabecera desplazable
        item(key = "cabecera") {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onVolver) {
                    Text("Volver a Dirección")
                }

                Text(
                    text = "Historial de acciones",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        // Estado de los registros
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

            estado.registros.isEmpty() -> {
                item {
                    Text("No hay acciones registradas")
                }
            }

            else -> {
                items(
                    items = estado.registros,
                    key = { it.id }
                ) { registro ->

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = registro.accion,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(registro.detalle)

                            Text("Responsable: ${registro.usuario}")

                            Text(
                                text = DateFormat
                                    .getDateTimeInstance()
                                    .format(Date(registro.fecha)),
                                style = MaterialTheme.typography.bodySmall
                            )

                            registro.candidataId?.let {
                                Text("Candidatura N.º $it")
                            }
                        }
                    }
                }
            }
        }
    }
}
