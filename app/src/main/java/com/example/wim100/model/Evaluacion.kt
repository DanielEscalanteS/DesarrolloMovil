package com.example.wim100.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evaluaciones",
    foreignKeys = [
        ForeignKey(
            entity = Candidata::class,
            parentColumns = ["id"],
            childColumns = ["candidataId"],
            onDelete = ForeignKey.CASCADE // Si borras la candidata, se borra su evaluación
        )
    ],
    // Esto asegura que una evaluadora no califique 2 veces a la misma persona
    indices = [Index(value = ["candidataId", "evaluadoraClave"], unique = true)]
)
data class Evaluacion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val candidataId: Long,
    val evaluadora: String,
    val evaluadoraClave: String,
    val puntajeTotal: Double,
    val observacionGeneral: String,
    val fecha: Long = System.currentTimeMillis()
) {
    companion object {
        const val MINIMO_PARA_CONSOLIDAR = 2

        // "  AnA   PérEz " se convierte en "ana pérez" para evitar trucos
        fun normalizarClave(nombre: String): String =
            nombre.trim().lowercase().replace(Regex("\\s+"), " ")
    }
}