package com.example.wim100.ui.screens

import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.model.EstadoCandidatura
import com.example.wim100.viewmodel.CandidatasViewModel

@Composable
fun CandidatasScreen(
    viewModel: CandidatasViewModel,
    onVolver: () -> Unit,
    onEvaluar: (Long) -> Unit // Recibe la orden para viajar a la pantalla de notas
) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current // Necesario para pedirle al celular que abra el PDF

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onVolver) { Text("Volver") }
        Text("Bandeja de Evaluadoras", style = MaterialTheme.typography.headlineMedium)

        if (s.cargando) {
            CircularProgressIndicator()
        } else if (s.error != null) {
            Text(s.error.orEmpty(), color = MaterialTheme.colorScheme.error)
        } else if (s.candidatas.isEmpty()) {
            Text("Todavía no hay postulaciones.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(s.candidatas, key = { it.id }) { candidata ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Person,
                                    contentDescription = "Usuario",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(candidata.nombre, style = MaterialTheme.typography.titleLarge)
                            }
                            Text(candidata.area)
                            Text("Estado: ${candidata.estado.etiqueta}")
                            Text("CV: ${candidata.cvNombre ?: "Sin adjunto"}", style = MaterialTheme.typography.bodySmall)

                            Spacer(modifier = Modifier.height(8.dp))

                            // Fila para acomodar los dos botones uno al lado del otro
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                                // BOTÓN 1 - VER PDF
                                if (candidata.cvUri != null) {
                                    OutlinedButton(onClick = {
                                        try {
                                            val guardada = Uri.parse(candidata.cvUri)
                                            val uriCompartida = if (guardada.scheme == "file") {
                                                FileProvider.getUriForFile(context, "${context.packageName}.archivos", File(checkNotNull(guardada.path)))
                                            } else guardada
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uriCompartida, "application/pdf")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: android.content.ActivityNotFoundException) {
                                            Toast.makeText(context, "Instala una aplicación para abrir PDF", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No se pudo abrir el CV. Revisa que el archivo exista.", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Text("Ver CV")
                                    }
                                }

                                // BOTÓN 2 - EVALUAR (Solo aparece si no está resuelta)
                                if (candidata.estado == EstadoCandidatura.POSTULADA || candidata.estado == EstadoCandidatura.EN_EVALUACION) {
                                    Button(onClick = { onEvaluar(candidata.id) }) {
                                        Text("Evaluar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}