package com.example.wim100

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.wim100.data.local.Migraciones
import com.example.wim100.data.local.WimDatabase
import com.example.wim100.model.Evaluacion
import com.example.wim100.model.EstadoCandidatura
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MigracionRoomTest {
    @Test fun migraSinBorrarCandidatasYArchivaLosDuplicadosAntesDeExigirUnaEvaluacion() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val nombre = "migracion_${UUID.randomUUID()}.db"
        val esquema = instrumentation.context.assets.open("wim100_v1.sql").bufferedReader().use { it.readText() }
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(nombre).callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    esquema.split(';').filter { it.isNotBlank() }.forEach { db.execSQL(it) }
                    db.execSQL("INSERT INTO candidatas VALUES (1,'Persona Ficticia','prueba@example.com','912345678','Blue Team',2,'Perfil ficticio para probar migración','https://www.linkedin.com/in/prueba',NULL,NULL,'EN_EVALUACION',1,'Demo')")
                    db.execSQL("INSERT INTO criterios VALUES (1,'Trayectoria','Prueba',100,1)")
                    db.execSQL("INSERT INTO evaluaciones VALUES (1,1,'Ana','ana',80,'Primera observación ficticia',1)")
                    db.execSQL("INSERT INTO evaluaciones VALUES (2,1,'Beatriz','beatriz',40,'Segunda observación ficticia',2)")
                    db.execSQL("INSERT INTO puntajes_criterio VALUES (1,1,1,4,'Primera nota')")
                    db.execSQL("INSERT INTO puntajes_criterio VALUES (2,2,1,2,'Justificación anterior de Beatriz')")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build())
        try {
            helper.writableDatabase
            helper.close()
            val db = Room.databaseBuilder(context, WimDatabase::class.java, nombre)
                .addMigrations(Migraciones.MIGRATION_1_2).build()
            try {
                val ficha = checkNotNull(db.evaluacionDao().observarFicha(1).first())
                assertEquals(EstadoCandidatura.EVALUADA, ficha.candidata.estado)
                assertEquals(1, ficha.evaluaciones.size)
                assertEquals("Ana", ficha.evaluaciones.single().evaluacion.evaluadora)
                assertEquals(4, ficha.evaluaciones.single().puntajes.single().puntaje)
                val archivo = db.historialDao().observarPorCandidata(1).first().single()
                assertTrue(archivo.detalle.contains("Beatriz"))
                assertTrue(archivo.detalle.contains("Justificación anterior de Beatriz"))
                try {
                    db.evaluacionDao().insertar(Evaluacion(candidataId = 1, evaluadora = "Otra", evaluadoraClave = "otra",
                        puntajeTotal = 80.0, observacionGeneral = "Otra observación ficticia para la prueba"))
                    fail("El índice debe impedir una segunda evaluación incluso desde el DAO")
                } catch (expected: android.database.sqlite.SQLiteConstraintException) { }
            } finally { db.close() }
        } finally {
            helper.close()
            context.deleteDatabase(nombre)
        }
    }
}
