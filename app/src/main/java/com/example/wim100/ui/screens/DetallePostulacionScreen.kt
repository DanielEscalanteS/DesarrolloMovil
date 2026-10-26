
package com.example.wim100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wim100.model.Candidata
import com.example.wim100.ui.components.PantallaDesplazable
import java.text.DateFormat
import java.util.Date

@Composable
fun DetallePostulacionScreen(
    candidata: Candidata,
    onVolver: () -> Unit
) {
    PantallaDesplazable {

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onVolver) {
                    Text("Volver")
                }

                Text(
                    text = "Detalle de postulación",
                    style = MaterialTheme.typography.headlineSmall
                )

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
