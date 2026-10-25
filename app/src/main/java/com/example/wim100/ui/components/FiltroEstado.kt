package com.example.wim100.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wim100.model.EstadoCandidatura

@Composable
fun FiltroEstado(seleccionado: EstadoCandidatura?, onSeleccionar: (EstadoCandidatura?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = seleccionado == null, onClick = { onSeleccionar(null) }, label = { Text("Todas") })
        EstadoCandidatura.entries.forEach { estado ->
            FilterChip(selected = seleccionado == estado, onClick = { onSeleccionar(estado) }, label = { Text(estado.etiqueta) })
        }
    }
}
