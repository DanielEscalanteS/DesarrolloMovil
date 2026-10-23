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
}
