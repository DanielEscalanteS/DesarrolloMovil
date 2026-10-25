package com.example.wim100.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "criterios")
data class Criterio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val descripcion: String,
    val peso: Int, // Porcentaje (los 5 criterios suman los 100 totales)
    val orden: Int
)