package com.example.wim100.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.model.AREAS_ESPECIALIDAD
import com.example.wim100.nativo.ArchivoPostulacion
import com.example.wim100.ui.components.CampoFormulario
import com.example.wim100.viewmodel.FormularioViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.Error
import androidx.compose.animation.AnimatedContent
import androidx.compose.material.icons.filled.CheckCircle



@Composable
fun PostulacionScreen(viewModel: FormularioViewModel, onVolver: () -> Unit) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importando by remember { mutableStateOf(false) }
    var menuArea by remember { mutableStateOf(false) }
    val selector =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                importando = true
                scope.launch {
                    try {
                        val cv = ArchivoPostulacion.importar(context.applicationContext, uri)
                        viewModel.actualizarCv(cv.uri, cv.nombre, cv.tamano)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        viewModel.informarErrorArchivo(e.message ?: "No se pudo leer el PDF")
                    } finally {
                        importando = false
                    }
                }
            }
        }
    val habilitado = !s.guardando && !importando && s.idGuardado == null

    if (s.idGuardado != null) {
        AlertDialog(
            onDismissRequest = {},

            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },

            title = {
                Text(
                    text = "¡Postulación enviada!",
                    style = MaterialTheme.typography.headlineSmall
                )
            },

            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Tu postulación se ha registrado correctamente.",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Número de postulación",
                                style = MaterialTheme.typography.labelMedium
                            )

                            Text(
                                text = "N.º ${s.idGuardado}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = "Tus datos quedaron guardados localmente en este dispositivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },

            confirmButton = {
                Button(
                    onClick = {
                        viewModel.limpiarFormulario()
                        onVolver()
                    }
                ) {
                    Text("Finalizar")
                }
            }
        )
    }
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onVolver, enabled = !s.guardando && !importando) { Text("Volver") }
        //Text("Mi postulación", style = MaterialTheme.typography.headlineMedium)
        //Text("Completa tus antecedentes. Usa datos ficticios y un CV de prueba.")
        Text(
            text = "Mi postulación",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        ElevatedCard(modifier = Modifier.fillMaxWidth())
        {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Antes de comenzar",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Completa tus datos personales y antecedentes profesionales para registrar tu postulación.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Utiliza información ficticia y adjunta un CV de prueba en formato PDF (máximo 5 MB).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text("Datos personales", style = MaterialTheme.typography.titleMedium)

        CampoFormulario(
            valor = s.nombre,
            etiqueta = "Nombre completo",
            error = s.errores.nombre,
            onCambio = viewModel::actualizarNombre,
            habilitado = habilitado,
            icono = Icons.Default.Person
        )

        CampoFormulario(
            valor = s.correo,
            etiqueta = "Correo de prueba (@example.com)",
            error = s.errores.correo,
            onCambio = viewModel::actualizarCorreo,
            habilitado = habilitado,
            teclado = KeyboardType.Email,
            icono = Icons.Default.Email
        )

        CampoFormulario(
            valor = s.telefono,
            etiqueta = "Teléfono",
            error = s.errores.telefono,
            onCambio = viewModel::actualizarTelefono,
            habilitado = habilitado,
            teclado = KeyboardType.Phone,
            icono = Icons.Default.Phone
        )

        Text("Antecedentes profesionales", style = MaterialTheme.typography.titleMedium)
        Box {
            OutlinedButton(
                onClick = { menuArea = true },
                enabled = habilitado,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(s.area.ifBlank { "Seleccionar área" })
            }
            DropdownMenu(expanded = menuArea, onDismissRequest = { menuArea = false }) {
                AREAS_ESPECIALIDAD.forEach { area ->
                    DropdownMenuItem(
                        text = { Text(area) },
                        onClick = { viewModel.actualizarArea(area); menuArea = false })
                }
            }
        }
        s.errores.area?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        CampoFormulario(
            valor = s.aniosExperiencia,
            etiqueta = "Años de experiencia",
            error = s.errores.aniosExperiencia,
            onCambio = viewModel::actualizarAnios,
            habilitado = habilitado,
            teclado = KeyboardType.Number,
            icono = Icons.Default.Work
        )

        CampoFormulario(
            valor = s.resumenExperiencia,
            etiqueta = "Resumen de experiencia (mín. 30 caracteres)",
            error = s.errores.resumenExperiencia,
            onCambio = viewModel::actualizarResumen,
            habilitado = habilitado,
            multilinea = true,
            icono = Icons.Default.Description
        )

        CampoFormulario(
            valor = s.linkedinUrl,
            etiqueta = "Enlace de perfil ficticio (LinkedIn)",
            error = s.errores.linkedinUrl,
            onCambio = viewModel::actualizarLinkedin,
            habilitado = habilitado,
            teclado = KeyboardType.Uri,
            icono = Icons.Default.Link
        )
        Text(
            text = "Currículum vitae",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedButton(
            onClick = { selector.launch(arrayOf("application/pdf")) },
            enabled = habilitado,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (s.cvUri == null)
                    "Adjuntar CV en PDF"
                else
                    "Cambiar CV"
            )
        }

        AnimatedVisibility(visible = s.cvUri != null) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.cvNombre ?: "Documento PDF",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(text = s.cvTamano?.let {
                            "Tamaño: %.2f MB".format(
                                it / (1024.0 * 1024.0)
                            )
                        } ?: "Archivo seleccionado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (s.cvUri == null) {
            Text(
                text = "Selecciona un PDF de prueba (máximo 5 MB).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = s.errores.cv != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = s.errores.cv.orEmpty(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        AnimatedVisibility(visible = s.errorGuardado != null) {
            Text(
                s.errorGuardado.orEmpty(),
                color = MaterialTheme.colorScheme.error
            )
        }
        Button(
            onClick = viewModel::enviarPostulacion,
            enabled = habilitado,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (s.guardando || importando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            AnimatedContent(
                targetState = when {
                    s.guardando -> "Guardando…"
                    importando -> "Leyendo PDF…"
                    else -> "Enviar postulación"
                },
                label = "AnimacionEstadoPostulacion"
            ) { texto ->
                Text(text = texto)
            }
        }
    }
}