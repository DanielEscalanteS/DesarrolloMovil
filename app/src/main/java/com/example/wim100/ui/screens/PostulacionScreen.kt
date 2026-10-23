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

@Composable
fun PostulacionScreen(viewModel: FormularioViewModel, onVolver: () -> Unit) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importando by remember { mutableStateOf(false) }
    var menuArea by remember { mutableStateOf(false) }
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
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
                } finally { importando = false }
            }
        }
    }
    val habilitado = !s.guardando && !importando && s.idGuardado == null
    if (s.idGuardado != null) {
        AlertDialog(
            onDismissRequest = {}, title = { Text("Postulación recibida") },
            text = { Text("Tu postulación N.º ${s.idGuardado} quedó guardada en este dispositivo.") },
            confirmButton = { TextButton(onClick = { viewModel.limpiarFormulario(); onVolver() }) { Text("Aceptar") } }
        )
    }
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onVolver, enabled = !s.guardando && !importando) { Text("Volver") }
        Text("Mi postulación", style = MaterialTheme.typography.headlineMedium)
        Text("Completa tus antecedentes. Usa datos ficticios y un CV de prueba.")
        Text("Datos personales", style = MaterialTheme.typography.titleMedium)
        CampoFormulario(s.nombre, "Nombre completo", s.errores.nombre, viewModel::actualizarNombre, habilitado)
        CampoFormulario(s.correo, "Correo de prueba (@example.com)", s.errores.correo, viewModel::actualizarCorreo, habilitado, teclado = KeyboardType.Email)
        CampoFormulario(s.telefono, "Teléfono", s.errores.telefono, viewModel::actualizarTelefono, habilitado, teclado = KeyboardType.Phone)
        Text("Antecedentes profesionales", style = MaterialTheme.typography.titleMedium)
        Box {
            OutlinedButton(onClick = { menuArea = true }, enabled = habilitado, modifier = Modifier.fillMaxWidth()) {
                Text(s.area.ifBlank { "Seleccionar área" })
            }
            DropdownMenu(expanded = menuArea, onDismissRequest = { menuArea = false }) {
                AREAS_ESPECIALIDAD.forEach { area ->
                    DropdownMenuItem(text = { Text(area) }, onClick = { viewModel.actualizarArea(area); menuArea = false })
                }
            }
        }
        s.errores.area?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        CampoFormulario(s.aniosExperiencia, "Años de experiencia", s.errores.aniosExperiencia, viewModel::actualizarAnios, habilitado, teclado = KeyboardType.Number)
        CampoFormulario(s.resumenExperiencia, "Resumen de experiencia (mín. 30 caracteres)", s.errores.resumenExperiencia, viewModel::actualizarResumen, habilitado, multilinea = true)
        CampoFormulario(s.linkedinUrl, "Enlace de perfil ficticio (https://www.linkedin.com/in/...)", s.errores.linkedinUrl, viewModel::actualizarLinkedin, habilitado, teclado = KeyboardType.Uri)
        OutlinedButton(onClick = { selector.launch(arrayOf("application/pdf")) }, enabled = habilitado, modifier = Modifier.fillMaxWidth()) {
            Text(if (s.cvNombre == null) "Adjuntar CV en PDF" else "Cambiar CV")
        }
        Text(s.cvNombre ?: "PDF de prueba, hasta 5 MB", style = MaterialTheme.typography.bodySmall)
        s.errores.cv?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        AnimatedVisibility(visible = s.errorGuardado != null) {
            Text(s.errorGuardado.orEmpty(), color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = viewModel::enviarPostulacion, enabled = habilitado, modifier = Modifier.fillMaxWidth()) {
            if (s.guardando || importando) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (s.guardando) "Guardando…" else if (importando) "Leyendo PDF…" else "Enviar postulación")
        }
        Text("Los datos se guardan localmente; las vistas comparten este dispositivo.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
    }
}
