package com.example.wim100.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.viewmodel.DatosEjemploViewModel

@Composable
fun DatosEjemploScreen(viewModel: DatosEjemploViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var reemplazarId by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmar by remember { mutableStateOf(false) }
    val anterior = estado.registrosAnteriores.find { it.id == reemplazarId }
    BackHandler(estado.guardando) { }

    if (confirmar && anterior != null) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("Reemplazar registro de prueba") },
            text = { Text("Se quitará a ${anterior.nombre}, sus evaluaciones, su historial asociado y su CV privado (${anterior.cvNombre ?: "sin archivo"}). Se agregarán los ejemplos que falten. Los demás registros se conservarán.") },
            confirmButton = { TextButton(onClick = { viewModel.cargar(anterior.id); confirmar = false; reemplazarId = null }) { Text("Reemplazar") } },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onVolver, enabled = !estado.guardando) { Text("Volver") }
        Text("Datos de ejemplo", style = MaterialTheme.typography.headlineSmall)
        if (estado.cargando) CircularProgressIndicator()
        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Camila Rojas: sin evaluar.")
                Text("Valentina Soto: evaluada por Ana Pérez.")
                Text("Francisca Muñoz: evaluada por Ana Pérez y Beatriz López.")
                Text("Cada candidata incluye un CV ficticio. Ingresa como Ana Pérez o Beatriz López para probar «Ya evaluaste».", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Text("Registro anterior", style = MaterialTheme.typography.titleMedium)
                Text("Si quieres retirar la prueba anterior, selecciónala aquí. Revisa su nombre y archivo antes de reemplazarla.")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = anterior == null, enabled = !estado.guardando, onClick = { reemplazarId = null })
                    Text("Conservar los registros actuales", Modifier.weight(1f))
                }
            }
            items(estado.registrosAnteriores, key = { it.id }) { candidata ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = anterior?.id == candidata.id, enabled = !estado.guardando, onClick = { reemplazarId = candidata.id })
                        Column(Modifier.weight(1f)) {
                            Text(candidata.nombre, style = MaterialTheme.typography.titleSmall)
                            Text(candidata.correo)
                            Text("CV: ${candidata.cvNombre ?: "Sin archivo"}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                estado.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(enabled = !estado.guardando, modifier = Modifier.fillMaxWidth(), onClick = {
                    if (anterior != null) confirmar = true else viewModel.cargar(null)
                }) { Text(if (estado.guardando) "Preparando ejemplos…" else if (anterior != null) "Reemplazar y cargar ejemplos" else "Cargar ejemplos") }
                Text("Volver a cargar no duplica los ejemplos ni borra los avances que hagas en ellos.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
