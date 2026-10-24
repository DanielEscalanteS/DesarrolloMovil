package com.example.wim100.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Variable global para todo el proyecto
val AREAS_ESPECIALIDAD = listOf(
    "Seguridad ofensiva (Red Team)",
    "Seguridad defensiva (Blue Team)",
    "Gobierno, riesgo y cumplimiento",
    "Ciberseguridad industrial (OT)",
    "Forense y respuesta a incidentes",
    "Investigación y academia"
)

@Entity(
    tableName = "candidatas",
    indices = [Index(value = ["correo"], unique = true)]
)
data class Candidata(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val correo: String,
    val telefono: String,
    val area: String,
    val aniosExperiencia: Int,
    val resumenExperiencia: String,
    val linkedinUrl: String,
    val cvUri: String?,
    val cvNombre: String?,
    val estado: EstadoCandidatura = EstadoCandidatura.POSTULADA,
    val fechaRegistro: Long = System.currentTimeMillis(),
    val registradaPor: String
)