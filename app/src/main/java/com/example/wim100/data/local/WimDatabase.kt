package com.example.wim100.data.local

import android.content.Context
import com.example.wim100.data.local.dao.CandidataDao
import com.example.wim100.data.local.dao.CriterioDao
import com.example.wim100.data.local.dao.EvaluacionDao
import com.example.wim100.data.local.dao.HistorialDao
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.wim100.model.Candidata
import com.example.wim100.model.Criterio
import com.example.wim100.model.Evaluacion
import com.example.wim100.model.PuntajeCriterio
import com.example.wim100.model.RegistroHistorial

@Database(
    entities = [
        Candidata::class,
        Criterio::class,
        Evaluacion::class,
        PuntajeCriterio::class,
        RegistroHistorial::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WimDatabase : RoomDatabase() {
    abstract fun candidataDao(): CandidataDao
    abstract fun criterioDao(): CriterioDao
    abstract fun evaluacionDao(): EvaluacionDao
    abstract fun historialDao(): HistorialDao


    companion object {
        @Volatile
        private var instancia: WimDatabase? = null

        fun obtener(context: Context): WimDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    WimDatabase::class.java,
                    "wim100.db"
                )
                    .addMigrations(Migraciones.MIGRATION_1_2)
                    .build()
                    .also { instancia = it }
            }
    }
}
