package com.example.wim100.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.wim100.model.*

@Composable
fun ResumenPuntaje(puntaje: Double) {
    val progreso by animateFloatAsState((puntaje / 100.0).toFloat().coerceIn(0f, 1f), label = "Puntaje total")
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxSize(), strokeWidth = 7.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(String.format("%.1f", puntaje), style = MaterialTheme.typography.headlineSmall)
                    Text("/ 100", style = MaterialTheme.typography.labelMedium)
                }
            }
            Column(Modifier.weight(1f)) {
                Text("Puntaje total", style = MaterialTheme.typography.titleMedium)
                Text(CalculadoraPuntaje.categoria(puntaje), color = MaterialTheme.colorScheme.primary)
                Text("Resultado ponderado de los cinco criterios", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun FortalezasEvaluacion(criterios: List<Criterio>, puntajes: List<PuntajeCriterio>) {
    val resumen = AnalisisEvaluacion.resumir(criterios, puntajes.associate { it.criterioId to it.puntaje })
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Perfil según la evaluación", style = MaterialTheme.typography.titleMedium)
        GrupoCriterios("Fortalezas · notas 4 y 5", resumen.fortalezas, Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary)
        GrupoCriterios("Nivel adecuado · nota 3", resumen.adecuados, Icons.Default.Info, MaterialTheme.colorScheme.onSurfaceVariant)
        GrupoCriterios("Por mejorar · notas 1 y 2", resumen.porMejorar, Icons.Default.Warning, MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun GrupoCriterios(titulo: String, criterios: List<String>, icono: ImageVector, color: Color) {
    if (criterios.isEmpty()) return

    Surface(Modifier.fillMaxWidth(), color = color.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icono, contentDescription = null, tint = color)
            Column(Modifier.weight(1f)) {
                Text(titulo, color = color, style = MaterialTheme.typography.labelLarge)
                Text(criterios.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun BarraNota(nombre: String, nota: Int?, observacion: String = "") {
    val progreso by animateFloatAsState(((nota ?: 0) / 5f).coerceIn(0f, 1f), label = "Nota por criterio")
    val color = if (nota != null && nota <= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(nombre, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                Text(nota?.let { "$it / 5" } ?: "—", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = color)
            }
        }
        LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth().height(8.dp), color = color)
        Text(AnalisisEvaluacion.etiqueta(nota), color = color, style = MaterialTheme.typography.labelMedium)
        if (observacion.isNotBlank()) Text("Justificación: $observacion", style = MaterialTheme.typography.bodyMedium)
    }
}
