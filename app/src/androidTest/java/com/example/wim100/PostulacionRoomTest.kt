package com.example.wim100

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.wim100.data.local.WimDatabase
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.HistorialRepository
import com.example.wim100.model.Candidata
import com.example.wim100.model.RegistroHistorial
import com.example.wim100.model.EstadoCandidatura
import com.example.wim100.model.Evaluacion
import com.example.wim100.model.Rol
import com.example.wim100.model.Sesion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostulacionRoomTest {
    private lateinit var db: WimDatabase
    private lateinit var repository: CandidataRepository

    @Before fun preparar() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, WimDatabase::class.java
        ).build()
        repository = CandidataRepository(db.candidataDao(), HistorialRepository(db.historialDao()))
    }
    @After fun cerrar() { db.close() }

    private fun candidata(correo: String = "persona@example.com") = Candidata(
        nombre = "Persona Ficticia", correo = correo, telefono = "912345678",
        area = "Seguridad defensiva (Blue Team)", aniosExperiencia = 2,
        resumenExperiencia = "Antecedentes ficticios para una prueba de persistencia local.",
        linkedinUrl = "https://www.linkedin.com/in/persona-ficticia", cvUri = "file:///prueba.pdf",
        cvNombre = "prueba.pdf", registradaPor = "Persona Ficticia"
    )

    @Test fun registrarGuardaCandidataYAsociaSuHistorial() = runBlocking {
        val id = repository.registrar(candidata())
        assertNotNull(repository.obtener(id))
        assertEquals(id, db.historialDao().observarTodo().first().single().candidataId)
    }

    @Test fun correoDuplicadoNoGeneraOtraCandidataNiOtroHistorial() = runBlocking {
        repository.registrar(candidata())
        try {
            repository.registrar(candidata(" PERSONA@EXAMPLE.COM "))
            fail("Debió rechazar el correo duplicado")
        } catch (expected: IllegalArgumentException) {
            assertEquals(1, repository.contar())
            assertEquals(1, db.historialDao().observarTodo().first().size)
        }
    }

    @Test fun falloDeHistorialRevierteInsercionDeCandidata() = runBlocking {
        val registro = RegistroHistorial(id = 9, candidataId = null, accion = "Prueba", detalle = "Prueba", usuario = "Demo")
        db.historialDao().insertar(registro)
        try {
            db.candidataDao().insertarConHistorial(candidata(), registro)
            fail("Debió fallar por clave de historial duplicada")
        } catch (expected: SQLiteConstraintException) {
            assertEquals(0, repository.contar())
            assertEquals(1, db.historialDao().observarTodo().first().size)
        }
    }

    private suspend fun prepararEvaluada(): Long {
        val id = repository.registrar(candidata())
        for (numero in 1..Evaluacion.MINIMO_PARA_CONSOLIDAR) {
            db.evaluacionDao().insertar(Evaluacion(
                candidataId = id, evaluadora = "Evaluadora $numero",
                evaluadoraClave = "evaluadora $numero", puntajeTotal = 80.0,
                observacionGeneral = "Observación ficticia suficiente para la prueba"
            ))
        }
        db.candidataDao().actualizarEstado(id, EstadoCandidatura.EVALUADA)
        return id
    }

    @Test fun soloDireccionPuedeResolver() = runBlocking {
        val id = prepararEvaluada()
        try {
            repository.resolver(id, EstadoCandidatura.SELECCIONADA, Sesion("Demo", Rol.EVALUADORA))
            fail("Debió rechazar el rol")
        } catch (expected: IllegalArgumentException) {
            assertEquals(EstadoCandidatura.EVALUADA, repository.obtener(id)?.estado)
        }
    }

    @Test fun postuladaNoPuedeResolverse() = runBlocking {
        val id = repository.registrar(candidata())
        try {
            repository.resolver(id, EstadoCandidatura.SELECCIONADA, Sesion("Dirección Demo", Rol.DIRECCION))
            fail("Debió exigir evaluación")
        } catch (expected: IllegalStateException) {
            assertEquals(EstadoCandidatura.POSTULADA, repository.obtener(id)?.estado)
            assertEquals(1, db.historialDao().observarTodo().first().size)
        }
    }

    @Test fun resolverGuardaResponsableYBloqueaSegundaResolucion() = runBlocking {
        val id = prepararEvaluada()
        val sesion = Sesion("Dirección Demo", Rol.DIRECCION)
        repository.resolver(id, EstadoCandidatura.SELECCIONADA, sesion)
        assertEquals(EstadoCandidatura.SELECCIONADA, repository.obtener(id)?.estado)
        assertEquals("Dirección Demo", db.historialDao().observarTodo().first().first().usuario)
        try {
            repository.resolver(id, EstadoCandidatura.NO_SELECCIONADA, sesion)
            fail("No debe sobrescribir una resolución")
        } catch (expected: IllegalStateException) {
            assertEquals(EstadoCandidatura.SELECCIONADA, repository.obtener(id)?.estado)
        }
    }

    @Test fun falloDeHistorialRevierteResolucion() = runBlocking {
        val id = prepararEvaluada()
        val registro = RegistroHistorial(id = 99, candidataId = id, accion = "Prueba", detalle = "Prueba", usuario = "Demo")
        db.historialDao().insertar(registro)
        try {
            db.candidataDao().resolverConHistorial(id, EstadoCandidatura.SELECCIONADA, registro, Evaluacion.MINIMO_PARA_CONSOLIDAR)
            fail("Debe fallar la inserción duplicada del historial")
        } catch (expected: SQLiteConstraintException) {
            assertEquals(EstadoCandidatura.EVALUADA, repository.obtener(id)?.estado)
        }
    }
}
