package com.example.wim100.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> SelectorOpcion(titulo: String, valor: T, opciones: List<T>, etiqueta: (T) -> String, onCambio: (T) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$titulo: ${etiqueta(valor)}", Modifier.weight(1f))
            Icon(Icons.Default.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }, modifier = Modifier.heightIn(max = 320.dp)) {
            opciones.forEach { opcion ->
                DropdownMenuItem(text = { Text(etiqueta(opcion)) }, onClick = { onCambio(opcion); abierto = false })
            }
        }
    }
}
