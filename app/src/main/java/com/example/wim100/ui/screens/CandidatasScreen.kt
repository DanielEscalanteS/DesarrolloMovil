
package com.example.wim100.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.model.EstadoCandidatura
import com.example.wim100.ui.components.FiltroEstado
import com.example.wim100.viewmodel.CandidatasViewModel
import com.example.wim100.ui.components.PantallaDesplazable
import java.io.File

@Composable
fun CandidatasScreen(
    viewModel: CandidatasViewModel,
    nombreEvaluadora: String,
    onVolver: () -> Unit,
    onVerEvaluaciones: (Long) -> Unit,
    onEvaluar: (Long) -> Unit
) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var filtro by rememberSaveable {
        mutableStateOf<EstadoCandidatura?>(null)
    }

    val candidatasFiltradas = s.candidatas.filter {
        filtro == null || it.estado == filtro
    }

    // Toda la pantalla ahora se puede desplazar.
    PantallaDesplazable {

        // Cabecera desplazable.
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onVolver) {
                    Text("Volver")
                }

                Text(
                    text = "Bandeja de Evaluadoras",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text("Evaluadora: $nombreEvaluadora")

                FiltroEstado(filtro) {
                    filtro = it
                }
            }
        }

        // Estados del listado.
        when {
            s.cargando -> {
                item {
                    CircularProgressIndicator()
                }
            }

            s.error != null -> {
                item {
                    Text(
                        text = s.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            s.candidatas.isEmpty() -> {
                item {
                    Text("Todavía no hay postulaciones.")
                }
            }

            candidatasFiltradas.isEmpty() -> {
                item {
                    Text("No hay candidatas con este estado.")
                }
            }

            else -> {
                items(
                    items = candidatasFiltradas,
                    key = { it.id }
                ) { candidata ->

                    val yaEvaluo = s.yaEvaluo(
                        candidata.id,
                        nombreEvaluadora
                    )

                    val evaluacion = s.evaluaciones.find {
                        it.candidataId == candidata.id
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Usuario",
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    text = candidata.nombre,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }

                            Text(candidata.area)

                            Text(
                                "Estado: ${candidata.estado.etiqueta}"
                            )

                            Text(
                                text = when {
                                    yaEvaluo -> "Ya evaluaste"
                                    evaluacion != null ->
                                        "Evaluada por ${evaluacion.evaluadora}"
                                    candidata.estado.estaResuelta ->
                                        "Candidatura resuelta"
                                    else -> "Pendiente de evaluación"
                                },
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge
                            )

                            Text(
                                text = "CV: ${candidata.cvNombre ?: "Sin adjunto"}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Abrir CV.
                                if (candidata.cvUri != null) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val guardada = Uri.parse(
                                                    candidata.cvUri
                                                )

                                                val uriCompartida =
                                                    if (guardada.scheme == "file") {
                                                        FileProvider.getUriForFile(
                                                            context,
                                                            "${context.packageName}.archivos",
                                                            File(checkNotNull(guardada.path))
                                                        )
                                                    } else {
                                                        guardada
                                                    }

                                                val intent = Intent(
                                                    Intent.ACTION_VIEW
                                                ).apply {
                                                    setDataAndType(
                                                        uriCompartida,
                                                        "application/pdf"
                                                    )
                                                    addFlags(
                                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                    )
                                                }

                                                context.startActivity(intent)
                                            } catch (e: android.content.ActivityNotFoundException) {
                                                Toast.makeText(
                                                    context,
                                                    "Instala una aplicación para abrir PDF",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "No se pudo abrir el CV.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    ) {
                                        Text("Ver CV")
                                    }
                                }

                                // Evaluar candidata.
                                if (!candidata.estado.estaResuelta &&
                                    evaluacion == null
                                ) {
                                    Button(
                                        onClick = {
                                            onEvaluar(candidata.id)
                                        }
                                    ) {
                                        Text("Evaluar")
                                    }
                                }

                                // Editar evaluación existente.
                                if (!candidata.estado.estaResuelta &&
                                    yaEvaluo
                                ) {
                                    IconButton(
                                        onClick = {
                                            onEvaluar(candidata.id)
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription =
                                                "Editar evaluación de ${candidata.nombre}"
                                        )
                                    }
                                }
                            }

                            TextButton(
                                onClick = {
                                    onVerEvaluaciones(candidata.id)
                                }
                            ) {
                                Text("Ver evaluación")
                            }
                        }
                    }
                }
            }
        }
    }
}

