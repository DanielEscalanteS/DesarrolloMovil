package com.example.wim100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.viewmodel.CandidatasViewModel

@Composable
fun CandidatasScreen(viewModel: CandidatasViewModel, onVolver: () -> Unit) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onVolver) { Text("Volver") }
        Text("Postulaciones recibidas", style = MaterialTheme.typography.headlineMedium)
        Text("Bandeja local de revisión", style = MaterialTheme.typography.bodyMedium)
        when {
            s.cargando -> CircularProgressIndicator()
            s.error != null -> Text(s.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            s.candidatas.isEmpty() -> Text("Todavía no hay postulaciones. Puedes enviar una desde el inicio.")
            else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(s.candidatas, key = { it.id }) { candidata ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(candidata.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(candidata.area)
                            Text(candidata.correo)
                            Text("${candidata.aniosExperiencia} años de experiencia")
                            Text(candidata.resumenExperiencia)
                            Text("Estado: ${candidata.estado.etiqueta}")
                            Text("CV: ${candidata.cvNombre ?: "Sin adjunto"}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
