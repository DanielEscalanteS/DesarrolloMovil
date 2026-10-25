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

    @Test fun consultaDistingueResponsablesYConservaPuntajesTrasResolver() = runBlocking {
        ejemplos.cargar(null)
        val candidata = candidatas.observarTodas().first().single { it.nombre == "Francisca Muñoz" }
        val ficha = checkNotNull(evaluaciones.observarFicha(candidata.id).first())
        val liderazgo = evaluaciones.obtenerCriterios().single { it.nombre == "Liderazgo" }
        assertEquals(2, ficha.evaluaciones.size)
        val ana = ficha.evaluaciones.single { it.evaluacion.evaluadora == "Ana Pérez" }
        val beatriz = ficha.evaluaciones.single { it.evaluacion.evaluadora == "Beatriz López" }
        assertEquals(88.0, ana.evaluacion.puntajeTotal, 0.001)
        assertEquals(64.0, beatriz.evaluacion.puntajeTotal, 0.001)
        assertEquals(4, ana.puntajes.single { it.criterioId == liderazgo.id }.puntaje)
        assertEquals(2, beatriz.puntajes.single { it.criterioId == liderazgo.id }.puntaje)
        assertTrue(beatriz.puntajes.single { it.criterioId == liderazgo.id }.observacion.isNotBlank())
        assertTrue(ficha.evaluaciones.all { detalle -> detalle.puntajes.size == 5 && detalle.puntajes.all { it.evaluacionId == detalle.evaluacion.id } })
        val estado = CandidatasUiState(evaluaciones = evaluaciones.observarTodas().first())
        assertTrue(estado.yaEvaluo(candidata.id, "  AnA   PÉREZ "))
        assertFalse(estado.yaEvaluo(candidata.id, "Otra Evaluadora"))
        assertEquals(2, estado.totalEvaluaciones(candidata.id))

        candidatas.resolver(candidata.id, EstadoCandidatura.SELECCIONADA, Sesion("Dirección Demo", Rol.DIRECCION))
        val resuelta = checkNotNull(evaluaciones.observarFicha(candidata.id).first())
        assertEquals(EstadoCandidatura.SELECCIONADA, resuelta.candidata.estado)
        assertEquals(ana, resuelta.evaluaciones.single { it.evaluacion.id == ana.evaluacion.id })
    }

    @Test fun ejemplosNoSeDuplicanNiReemplazanLosAvancesYLosCvSonPdf() = runBlocking {
        ejemplos.cargar(null)
        val lista = candidatas.observarTodas().first()
        val camila = lista.single { it.nombre == "Camila Rojas" }
        assertTrue(checkNotNull(evaluaciones.observarFicha(camila.id).first()).evaluaciones.isEmpty())
        val criterios = evaluaciones.obtenerCriterios()
        evaluaciones.registrar(camila.id, "Beatriz López", criterios.associate { it.id to 4 }, emptyMap(), "Observación ficticia suficiente para registrar esta evaluación.")
        ejemplos.cargar(null)
        assertEquals(3, candidatas.contar())
        assertTrue(evaluaciones.yaEvaluo(camila.id, "Beatriz López"))
        assertEquals(4, evaluaciones.observarTodas().first().size)
        lista.forEach { candidata ->
            val archivo = File(checkNotNull(Uri.parse(candidata.cvUri).path))
            ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { pdf -> assertEquals(1, pdf.pageCount) }
            }
        }
    }

    private suspend fun registrarAnterior(nombre: String, correo: String, archivo: File): Long = candidatas.registrar(Candidata(
        nombre = nombre, correo = correo, telefono = "912345678", area = AREAS_ESPECIALIDAD[1], aniosExperiencia = 2,
        resumenExperiencia = "Perfil ficticio anterior para probar un reemplazo selectivo.",
        linkedinUrl = "https://www.linkedin.com/in/prueba", cvUri = Uri.fromFile(archivo).toString(),
        cvNombre = archivo.name, registradaPor = "Prueba anterior"
    ))

    @Test fun reemplazoSoloRetiraElRegistroElegidoSusNotasYSuArchivo() = runBlocking {
        ejemplos.cargar(null)
        val archivo = File(carpeta, "cvs/anterior.pdf").apply { writeText("Prueba anterior") }
        val otroArchivo = File(carpeta, "cvs/conservar.pdf").apply { writeText("Conservar") }
        val anterior = registrarAnterior("Prueba anterior", "anterior@example.com", archivo)
        val otro = registrarAnterior("Otra prueba", "otra@example.com", otroArchivo)
        val criterios = evaluaciones.obtenerCriterios()
        evaluaciones.registrar(anterior, "Ana Pérez", criterios.associate { it.id to 3 }, emptyMap(), "Observación ficticia de un registro que se retirará.")
        ejemplos.cargar(anterior)
        assertNull(candidatas.obtener(anterior))
        assertTrue(evaluaciones.observarPorCandidata(anterior).first().isEmpty())
        assertTrue(db.historialDao().observarPorCandidata(anterior).first().isEmpty())
        assertFalse(archivo.exists())
        assertNotNull(candidatas.obtener(otro))
        assertTrue(otroArchivo.exists())
        assertEquals(4, candidatas.contar())
    }

    @Test fun falloAlPrepararEjemplosRevierteElBorradoAnterior() = runBlocking {
        val archivo = File(carpeta, "cvs/anterior.pdf").apply { parentFile?.mkdirs(); writeText("Conservar al fallar") }
        val anterior = registrarAnterior("Prueba anterior", "anterior@example.com", archivo)
        db.criterioDao().insertarTodos(CriteriosDemostracion.lista.take(4))
        try {
            ejemplos.cargar(anterior)
            fail("La matriz incompleta debe impedir la carga")
        } catch (expected: IllegalStateException) {
            assertNotNull(candidatas.obtener(anterior))
            assertTrue(archivo.exists())
            assertEquals(1, candidatas.contar())
            assertEquals(1, db.historialDao().observarPorCandidata(anterior).first().size)
        }
    }
}
