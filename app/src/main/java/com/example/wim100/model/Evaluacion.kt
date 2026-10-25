package com.example.wim100.model

import androidx.room.Entity
import androidx.room.ColumnInfo
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
            onDelete = ForeignKey.CASCADE // Si se borra la candidata, se borra su evaluación
        )
    ],
    // Se conserva el índice anterior y se exige una sola evaluación por candidata.
    indices = [Index(value = ["candidataId", "evaluadoraClave"], unique = true),
        Index(value = ["candidataId"], unique = true)]
)
data class Evaluacion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val candidataId: Long,
    val evaluadora: String,
    val evaluadoraClave: String,
    val puntajeTotal: Double,
    val observacionGeneral: String,
    val fecha: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "NULL") val fechaEdicion: Long? = null,
    @ColumnInfo(defaultValue = "0") val revision: Int = 0
) {
    companion object {
        const val MINIMO_PARA_CONSOLIDAR = 1

        // "  AnA   PérEz " se convierte en "ana pérez" para evitar duplicados
        fun normalizarClave(nombre: String): String =
            nombre.trim().lowercase().replace(Regex("\\s+"), " ")
    }
}
