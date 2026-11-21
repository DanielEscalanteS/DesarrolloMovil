package com.example.wim100.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wim100.nativo.BiometriaHelper
import com.example.wim100.viewmodel.SesionViewModel

@Composable
fun LoginScreen(viewModel: SesionViewModel, onVolver: () -> Unit) {
    val estado by viewModel.loginState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(estado.requiereBiometria) {
        if (estado.requiereBiometria) {
            BiometriaHelper.autenticar(context,
                alExito = viewModel::confirmarBiometria,
                alError = { viewModel.cancelarBiometria("Acceso denegado: $it") })
        }
    }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onVolver, enabled = !estado.cargando) { Text("Volver al inicio") }
        Text("WIM 100", style = MaterialTheme.typography.displayMedium)
        Text("Gestión de talentos", style = MaterialTheme.typography.titleLarge)
        Text("Inicia sesión con tu cuenta de Evaluadora o Dirección.")
        OutlinedTextField(
            value = estado.usuario, onValueChange = viewModel::onUsuario,
            label = { Text("Usuario") }, singleLine = true, enabled = !estado.cargando,
            isError = estado.errorUsuario != null,
            supportingText = { estado.errorUsuario?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = estado.password, onValueChange = viewModel::onPassword,
            label = { Text("Contraseña") }, singleLine = true, enabled = !estado.cargando,
            visualTransformation = PasswordVisualTransformation(),
            isError = estado.errorPassword != null,
            supportingText = { estado.errorPassword?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth())
        estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = viewModel::iniciarSesion, enabled = !estado.cargando,
            modifier = Modifier.fillMaxWidth()) {
            if (estado.cargando) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (estado.cargando) "Verificando…" else "Ingresar")
        }
        Text("Dirección requiere además la verificación de seguridad del dispositivo.",
            style = MaterialTheme.typography.bodySmall)
        TextButton(enabled = !estado.cargando, onClick = {
            try { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
            catch (e: Exception) { Toast.makeText(context, "Abre Seguridad en los ajustes", Toast.LENGTH_SHORT).show() }
        }) { Text("Configurar seguridad del dispositivo") }
    }
}
