package com.example.wim100.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun CampoFormulario(
    valor: String,
    etiqueta: String,
    error: String?,
    onCambio: (String) -> Unit,
    habilitado: Boolean = true,
    multilinea: Boolean = false,
    teclado: KeyboardType = KeyboardType.Text,
    icono: ImageVector? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        modifier = Modifier.fillMaxWidth(),
        enabled = habilitado,
        isError = error != null,
        singleLine = !multilinea,
        minLines = if (multilinea) 3 else 1,
        keyboardOptions = KeyboardOptions(
            keyboardType = teclado
        ),
        leadingIcon = if (icono != null) {
            {
                Icon(
                    imageVector = icono,
                    contentDescription = null
                )
            }
        } else null,
        trailingIcon = if (error != null) {
            {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Campo con error"
                )
            }
        } else null,
        supportingText = if (error != null) {
            { Text(text = error) }
        } else null
    )
}