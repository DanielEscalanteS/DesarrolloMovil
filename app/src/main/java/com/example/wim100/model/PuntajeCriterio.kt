package com.example.wim100.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "puntajes_criterio",
    foreignKeys = [
        ForeignKey(
            entity = Evaluacion::class,
            parentColumns = ["id"],
            childColumns = ["evaluacionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Criterio::class,
            parentColumns = ["id"],
            childColumns = ["criterioId"]
        )
    ],
    indices = [Index("evaluacionId"), Index("criterioId")]
)
data class PuntajeCriterio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val evaluacionId: Long,
    val criterioId: Long,
    val puntaje: Int,
    val observacion: String
)

// Esta clase extra sirve para pedirle datos ordenados a la base de datos temporalmente
data class DetallePuntaje(
    val evaluacionId: Long,
    val criterioNombre: String,
    val peso: Int,
    val puntaje: Int,
    val observacion: String
)