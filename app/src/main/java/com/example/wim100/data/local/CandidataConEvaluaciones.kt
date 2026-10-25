package com.example.wim100.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.example.wim100.model.Candidata
import com.example.wim100.model.Evaluacion
import com.example.wim100.model.PuntajeCriterio

// Solo agrupan datos para consultarlos; no crean tablas nuevas.
data class EvaluacionConPuntajes(
    @Embedded val evaluacion: Evaluacion,
    @Relation(parentColumn = "id", entityColumn = "evaluacionId")
    val puntajes: List<PuntajeCriterio>
)

data class CandidataConEvaluaciones(
    @Embedded val candidata: Candidata,
    @Relation(parentColumn = "id", entityColumn = "candidataId", entity = Evaluacion::class)
    val evaluaciones: List<EvaluacionConPuntajes>
)
