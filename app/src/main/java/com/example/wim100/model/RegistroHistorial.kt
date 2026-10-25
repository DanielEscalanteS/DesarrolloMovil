package com.example.wim100.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "historial", indices = [Index("candidataId")])
data class RegistroHistorial(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val candidataId: Long?,
    val accion: String,
    val detalle: String,
    val usuario: String,
    val fecha: Long = System.currentTimeMillis()
)

object Acciones {
    const val INICIO_SESION = "Inicio de sesión"
    const val CIERRE_SESION = "Cierre de sesión"
    const val REGISTRO = "Registro de candidatura"
    const val EVALUACION = "Evaluación registrada"
    const val EDICION_EVALUACION = "Evaluación modificada"
    const val INTENTO_DUPLICADO = "Intento de evaluación duplicada"
    const val RESOLUCION = "Resolución de candidatura"
    const val CONSULTA_CV = "Consulta de CV"
    const val CONSULTA_PERFIL = "Consulta de perfil profesional"
    const val COMPARTIR = "Información compartida"
}
