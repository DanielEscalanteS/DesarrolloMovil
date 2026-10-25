package com.example.wim100

import android.content.Context
import android.content.ContextWrapper
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.wim100.data.local.WimDatabase
import com.example.wim100.data.repository.*
import com.example.wim100.model.*
import com.example.wim100.viewmodel.CandidatasUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ConsultaEvaluacionesRoomTest {
    private lateinit var db: WimDatabase
    private lateinit var candidatas: CandidataRepository
    private lateinit var evaluaciones: EvaluacionRepository
    private lateinit var ejemplos: DatosEjemploRepository
    private lateinit var carpeta: File

    @Before fun preparar() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        carpeta = File(base.cacheDir, "consulta_test_${UUID.randomUUID()}").apply { mkdirs() }
        val context: Context = object : ContextWrapper(base) { override fun getFilesDir(): File = carpeta }
        db = Room.inMemoryDatabaseBuilder(base, WimDatabase::class.java).build()
        val historial = HistorialRepository(db.historialDao())
        candidatas = CandidataRepository(db.candidataDao(), historial)
        evaluaciones = EvaluacionRepository(db, historial)
        ejemplos = DatosEjemploRepository(context, db, candidatas, evaluaciones, historial)
    }

    @After fun cerrar() { db.close(); carpeta.deleteRecursively() }

    @Test fun unaSolaEvaluacionPorCandidataAunqueCambieLaEvaluadora() = runBlocking {
        ejemplos.cargar()
        val candidata = candidatas.observarTodas().first().single { it.nombre == "Francisca Muñoz" }
        val ficha = checkNotNull(evaluaciones.obtenerFicha(candidata.id))
        assertEquals(1, ficha.evaluaciones.size)
        val estado = CandidatasUiState(evaluaciones = evaluaciones.observarTodas().first())
        assertTrue(estado.yaEvaluo(candidata.id, "  AnA   PÉREZ "))
        val criterios = evaluaciones.obtenerCriterios()
        try {
            evaluaciones.registrar(candidata.id, "Otra Evaluadora", criterios.associate { it.id to 4 }, emptyMap(), "Observación ficticia suficiente para la prueba.")
            fail("No debe permitir una segunda evaluación")
        } catch (expected: EvaluacionDuplicadaException) {
            assertEquals(1, evaluaciones.observarPorCandidata(candidata.id).first().size)
        }
    }

    @Test fun editarMantieneElRegistroOriginalYRechazaUnaRevisionDesactualizada() = runBlocking {
        ejemplos.cargar()
        val candidata = candidatas.observarTodas().first().single { it.nombre == "Francisca Muñoz" }
        val original = checkNotNull(evaluaciones.obtenerFicha(candidata.id)).evaluaciones.single().evaluacion
        val criterios = evaluaciones.obtenerCriterios()
        val notas = criterios.associate { it.id to 4 }
        evaluaciones.actualizar(candidata.id, Sesion("Ana Pérez", Rol.EVALUADORA), 0, notas, emptyMap(), "Se actualiza esta evaluación ficticia de prueba.")
        val actual = checkNotNull(evaluaciones.obtenerFicha(candidata.id)).evaluaciones.single()
        assertEquals(original.id, actual.evaluacion.id)
        assertEquals(original.fecha, actual.evaluacion.fecha)
        assertEquals(1, actual.evaluacion.revision)
        assertNotNull(actual.evaluacion.fechaEdicion)
        assertEquals(80.0, actual.evaluacion.puntajeTotal, 0.001)
        assertTrue(actual.puntajes.all { it.puntaje == 4 })
        assertTrue(db.historialDao().observarPorCandidata(candidata.id).first().any { it.accion == Acciones.EDICION_EVALUACION && it.detalle.contains("Liderazgo: 2 → 4") })
        try {
            evaluaciones.actualizar(candidata.id, Sesion("Ana Pérez", Rol.EVALUADORA), 0, notas, emptyMap(), "Otro intento con una revisión que ya quedó desactualizada.")
            fail("Debe rechazar una edición desactualizada")
        } catch (expected: IllegalStateException) {
            assertEquals(1, checkNotNull(evaluaciones.obtenerFicha(candidata.id)).evaluaciones.single().evaluacion.revision)
        }
    }

    @Test fun soloEditaSuResponsableAntesDeResolver() = runBlocking {
        ejemplos.cargar()
        val candidata = candidatas.observarTodas().first().single { it.nombre == "Francisca Muñoz" }
        val notas = evaluaciones.obtenerCriterios().associate { it.id to 4 }
        for (sesion in listOf(Sesion("Beatriz López", Rol.EVALUADORA), Sesion("Ana Pérez", Rol.DIRECCION))) {
            try {
                evaluaciones.actualizar(candidata.id, sesion, 0, notas, emptyMap(), "Observación ficticia suficiente para la prueba.")
                fail("Debe rechazar una edición sin permiso")
            } catch (expected: IllegalArgumentException) { }
        }
        candidatas.resolver(candidata.id, EstadoCandidatura.SELECCIONADA, Sesion("Dirección Demo", Rol.DIRECCION))
        assertEquals(1, checkNotNull(evaluaciones.obtenerFicha(candidata.id)).evaluaciones.size)
        try {
            evaluaciones.actualizar(candidata.id, Sesion("Ana Pérez", Rol.EVALUADORA), 0, notas, emptyMap(), "Observación ficticia suficiente para la prueba.")
            fail("Debe impedir editar tras resolver")
        } catch (expected: IllegalStateException) {
            assertEquals(0, checkNotNull(evaluaciones.obtenerFicha(candidata.id)).evaluaciones.single().evaluacion.revision)
        }
    }

    @Test fun falloDeHistorialRevierteLaEdicionYLosPuntajes() = runBlocking {
        ejemplos.cargar()
        val candidata = candidatas.observarTodas().first().single { it.nombre == "Francisca Muñoz" }
        val anterior = checkNotNull(evaluaciones.obtenerFicha(candidata.id)).evaluaciones.single()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER falla_edicion BEFORE INSERT ON historial WHEN NEW.accion = 'Evaluación modificada' BEGIN SELECT RAISE(ABORT, 'Fallo de prueba'); END")
        try {
            evaluaciones.actualizar(candidata.id, Sesion("Ana Pérez", Rol.EVALUADORA), 0,
                evaluaciones.obtenerCriterios().associate { it.id to 4 }, emptyMap(), "Observación ficticia suficiente para la prueba.")
            fail("El fallo del historial debe cancelar la edición")
        } catch (expected: android.database.sqlite.SQLiteException) {
            val despues = checkNotNull(evaluaciones.obtenerFicha(candidata.id)).evaluaciones.single()
            assertEquals(anterior.evaluacion, despues.evaluacion)
            assertEquals(anterior.puntajes.sortedBy { it.id }, despues.puntajes.sortedBy { it.id })
        }
    }

    @Test fun ejemplosAutomaticosNoDuplicanNiReemplazanAvancesYLosCvSonPdf() = runBlocking {
        ejemplos.cargar()
        val lista = candidatas.observarTodas().first()
        assertEquals(8, lista.size)
        assertEquals(2, lista.map { it.area }.distinct().size)
        val camila = lista.single { it.nombre == "Camila Rojas" }
        assertTrue(checkNotNull(evaluaciones.obtenerFicha(camila.id)).evaluaciones.isEmpty())
        evaluaciones.registrar(camila.id, "Beatriz López", evaluaciones.obtenerCriterios().associate { it.id to 4 }, emptyMap(), "Observación ficticia suficiente para registrar esta evaluación.")
        ejemplos.cargar()
        assertEquals(8, candidatas.contar())
        assertTrue(evaluaciones.yaEvaluo(camila.id, "Beatriz López"))
        assertEquals(8, evaluaciones.observarTodas().first().size)
        assertEquals(1, db.historialDao().contarAccion(DatosEjemploRepository.PREPARACION, DatosEjemploRepository.RESPONSABLE))
        lista.forEach { candidata ->
            val archivo = File(checkNotNull(Uri.parse(candidata.cvUri).path))
            ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { pdf -> assertEquals(1, pdf.pageCount) }
            }
        }
    }
}
