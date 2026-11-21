
package com.example.wim100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.wim100.model.Rol
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wim100.model.Candidata
import com.example.wim100.ui.components.PantallaDesplazable
import java.text.DateFormat
import java.util.Date
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun DetallePostulacionScreen(
    candidata: Candidata,
    onVolver: () -> Unit,
    rol: Rol,
    eliminando: Boolean,
    error: String?,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val context = LocalContext.current
    var confirmarEliminar by remember { mutableStateOf(false) }
    if (confirmarEliminar) {
        AlertDialog(onDismissRequest = { confirmarEliminar = false },
            title = { Text("¿Eliminar candidatura?") },
            text = { Text("Se eliminará del servidor y de este dispositivo, junto con sus evaluaciones locales.") },
            confirmButton = { TextButton(onClick = { confirmarEliminar = false; onEliminar() }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") } })
    }
    PantallaDesplazable {

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onVolver, enabled = !eliminando) {
                    Text("Volver")
                }

                Text(
                    text = "Detalle de postulación",
                    style = MaterialTheme.typography.headlineSmall
                )

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (rol == Rol.EVALUADORA && !candidata.estado.estaResuelta) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onEditar, enabled = !eliminando) { Text("Editar") }
                        TextButton(onClick = { confirmarEliminar = true }, enabled = !eliminando) {
                            Text(if (eliminando) "Eliminando…" else "Eliminar")
                        }
                    }
                }
                Text(
                    text = candidata.nombre,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Datos personales",
                        style = MaterialTheme.typography.titleMedium
                    )

                    DatoPostulacion("Nombre completo", candidata.nombre)
                    DatoPostulacion("Correo electrónico", candidata.correo)
                    DatoPostulacion("Teléfono", candidata.telefono)
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Antecedentes profesionales",
                        style = MaterialTheme.typography.titleMedium
                    )

                    DatoPostulacion(
                        "Área de especialidad",
                        candidata.area
                    )

                    DatoPostulacion(
                        "Años de experiencia",
                        "${candidata.aniosExperiencia}"
                    )

                    DatoPostulacion(
                        "Resumen de experiencia",
                        candidata.resumenExperiencia
                    )

                    DatoPostulacion(
                        "LinkedIn",
                        candidata.linkedinUrl
                    )
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Información de la postulación",
                        style = MaterialTheme.typography.titleMedium
                    )

                    DatoPostulacion("CV", candidata.cvNombre ?: "Sin archivo")
                    candidata.cvUri?.let { ruta ->
                        OutlinedButton(onClick = {
                            try {
                                val guardada = Uri.parse(ruta)
                                val uri = if (guardada.scheme == "file") FileProvider.getUriForFile(
                                    context, "${context.packageName}.archivos", File(checkNotNull(guardada.path))) else guardada
                                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                })
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo abrir el PDF. Revisa el archivo y la aplicación lectora.", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("Ver CV") }
                    }
                    if (candidata.cvUri == null && candidata.cvNombre != null) {
                        Text("El PDF no está disponible en este dispositivo.", style = MaterialTheme.typography.bodySmall)
                    }
                    DatoPostulacion("Registrada por", candidata.registradaPor)
                    DatoPostulacion(
                        "Número de postulación",
                        "${candidata.id}"
                    )

                    DatoPostulacion(
                        "Estado actual",
                        candidata.estado.etiqueta
                    )

                    DatoPostulacion(
                        "Fecha de registro",
                        DateFormat.getDateTimeInstance(
                            DateFormat.MEDIUM,
                            DateFormat.SHORT
                        ).format(Date(candidata.fechaRegistro))
                    )
                }
            }
        }
    }
}

@Composable
private fun DatoPostulacion(
    etiqueta: String,
    valor: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
