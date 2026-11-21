package com.example.wim100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InicioScreen(onPostular: () -> Unit, onIngresar: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("WIM 100", style = MaterialTheme.typography.displayMedium)
        Text("Gestión de talentos", style = MaterialTheme.typography.titleLarge)
        Text("Elige cómo quieres ingresar.")

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Person, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Text("Soy postulante", style = MaterialTheme.typography.headlineSmall)
                Text("Completa tus antecedentes y envía tu postulación sin crear una cuenta.")
                Button(onClick = onPostular, modifier = Modifier.fillMaxWidth()) {
                    Text("Enviar mi postulación")
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Work, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Text("Administración", style = MaterialTheme.typography.headlineSmall)
                Text("Acceso de Evaluadora y Dirección para revisar y gestionar candidaturas.")
                OutlinedButton(onClick = onIngresar, modifier = Modifier.fillMaxWidth()) {
                    Text("Iniciar sesión")
                }
            }
        }
    }
}
