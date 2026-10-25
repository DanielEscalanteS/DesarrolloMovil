package com.example.wim100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.viewmodel.HistorialViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun HistorialScreen(viewModel: HistorialViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onVolver) { Text("Volver a Dirección") }
        Text("Historial de acciones", style = MaterialTheme.typography.headlineMedium)
        when {
            estado.cargando -> CircularProgressIndicator()
            estado.error != null -> Text(estado.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            estado.registros.isEmpty() -> Text("No hay acciones registradas")
            else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(estado.registros, key = { it.id }) { registro ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(registro.accion, style = MaterialTheme.typography.titleMedium)
                            Text(registro.detalle)
                            Text("Responsable: ${registro.usuario}")
                            Text(DateFormat.getDateTimeInstance().format(Date(registro.fecha)), style = MaterialTheme.typography.bodySmall)
                            registro.candidataId?.let { Text("Candidatura N.º $it") }
                        }
                    }
                }
            }
        }
    }
}
