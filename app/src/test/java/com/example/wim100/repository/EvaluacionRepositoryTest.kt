package com.example.wim100.data.repository

import androidx.room.withTransaction
import com.example.wim100.data.local.WimDatabase
import com.example.wim100.data.local.dao.CandidataDao
import com.example.wim100.data.local.dao.CriterioDao
import com.example.wim100.data.local.dao.EvaluacionDao
import com.example.wim100.data.remote.EvaluacionDto
import com.example.wim100.data.remote.PuntajeDto
import com.example.wim100.data.remote.WimApi
import com.example.wim100.model.Acciones
import com.example.wim100.model.Criterio
import com.example.wim100.model.EstadoCandidatura
import com.example.wim100.model.Evaluacion
import com.example.wim100.model.PuntajeCriterio
import com.example.wim100.model.Rol
import com.example.wim100.model.Sesion
import com.example.wim100.viewmodel.candidataRemotaTest
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EvaluacionRepositoryTest {

    private val db = mockk<WimDatabase>()
    private val evaluacionDao = mockk<EvaluacionDao>(relaxed = true)
    private val criterioDao = mockk<CriterioDao>(relaxed = true)
    private val candidataDao = mockk<CandidataDao>(relaxed = true)
    private val historial = mockk<HistorialRepository>(relaxed = true)
    private val api = mockk<WimApi>()
    private lateinit var repository: EvaluacionRepository

    private val criterios = listOf(
        Criterio(id = 1L, nombre = "Trayectoria", descripcion = "Desc", peso = 50, orden = 1),
        Criterio(id = 2L, nombre = "Liderazgo", descripcion = "Desc", peso = 50, orden = 2)
    )

    @Before
    fun preparar() {
        every { db.evaluacionDao() } returns evaluacionDao
        every { db.criterioDao() } returns criterioDao
        every { db.candidataDao() } returns candidataDao
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            secondArg<suspend () -> Any?>().invoke()
        }
        repository = EvaluacionRepository(db, historial, api)
    }

    @After
    fun terminar() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun registrarCalculaPuntajeActualizaEstadoYRegistraHistorial() = runTest {
        coEvery { candidataDao.obtenerPorId(10L) } returns candidataRemotaTest(10L)
        coEvery { evaluacionDao.contarPorCandidata(10L) } returnsMany listOf(0, 1)
        coEvery { criterioDao.obtenerTodos() } returns criterios
        coEvery { evaluacionDao.insertar(any()) } returns 100L
        coEvery { evaluacionDao.insertarPuntajes(any()) } just Runs

        val total = repository.registrar(
            candidataId = 10L,
            evaluadora = "Camila Demo",
            puntajes = mapOf(1L to 5, 2L to 4),
            observaciones = emptyMap(),
            observacionGeneral = "Observación general suficientemente larga para aprobar."
        )

        assertEquals(90.0, total, 0.01)
        coVerify { candidataDao.actualizarEstado(10L, EstadoCandidatura.EVALUADA) }
        coVerify { historial.registrar(10L, Acciones.EVALUACION, any(), "Camila Demo") }
    }

    @Test
    fun registrarBloqueaEvaluacionDuplicadaYRegistraIntento() = runTest {
        coEvery { candidataDao.obtenerPorId(10L) } returns candidataRemotaTest(10L)
        coEvery { evaluacionDao.contarPorCandidata(10L) } returns 1

        val resultado = runCatching {
            repository.registrar(
                candidataId = 10L,
                evaluadora = "Otra Evaluadora",
                puntajes = mapOf(1L to 4, 2L to 4),
                observaciones = emptyMap(),
                observacionGeneral = "Observación general suficientemente larga para aprobar."
            )
        }

        assertTrue(resultado.exceptionOrNull() is EvaluacionDuplicadaException)
        coVerify { historial.registrar(10L, Acciones.INTENTO_DUPLICADO, any(), "Otra Evaluadora") }
    }

    @Test
    fun actualizarValidaResponsableRevisionYGuardaCambios() = runTest {
        val anterior = Evaluacion(
            id = 100L,
            candidataId = 10L,
            evaluadora = "Camila Demo",
            evaluadoraClave = "camila demo",
            puntajeTotal = 60.0,
            observacionGeneral = "Observación anterior válida de veinte caracteres.",
            revision = 0
        )
        coEvery { candidataDao.obtenerPorId(10L) } returns candidataRemotaTest(10L).copy(
            estado = EstadoCandidatura.EVALUADA
        )
        coEvery { evaluacionDao.obtenerPorCandidata(10L) } returns anterior
        coEvery { criterioDao.obtenerTodos() } returns criterios
        coEvery { evaluacionDao.obtenerPuntajes(100L) } returns listOf(
            PuntajeCriterio(1L, 100L, 1L, 3, "Ok"),
            PuntajeCriterio(2L, 100L, 2L, 3, "Ok")
        )
        coEvery { evaluacionDao.actualizar(100L, 80.0, any(), any(), 0) } returns 1

        val nuevoTotal = repository.actualizar(
            candidataId = 10L,
            sesion = Sesion("Camila Demo", Rol.EVALUADORA),
            revisionEsperada = 0,
            puntajes = mapOf(1L to 4, 2L to 4),
            observaciones = emptyMap(),
            observacionGeneral = "Nueva observación general válida de más de veinte caracteres."
        )

        assertEquals(80.0, nuevoTotal, 0.01)
        coVerify { historial.registrar(10L, Acciones.EDICION_EVALUACION, any(), "Camila Demo") }
    }

    @Test
    fun enviarYSincronizarConServidorTransformanDtosCorrectamente() = runTest {
        coEvery { criterioDao.obtenerTodos() } returns criterios
        coEvery { api.guardarEvaluacion(any()) } answers { firstArg() }

        repository.enviarAlServidor(
            candidataId = 10L,
            evaluadora = "Camila Demo",
            puntajes = mapOf(1L to 5, 2L to 5),
            observaciones = mapOf(1L to "Excelente"),
            observacionGeneral = "Observación general de prueba."
        )
        coVerify(exactly = 1) {
            api.guardarEvaluacion(match { it.postulanteId == 10L && it.puntajeTotal == 100.0 })
        }

        coEvery { api.listarEvaluaciones() } returns listOf(
            EvaluacionDto(
                id = 5L,
                postulanteId = 10L,
                evaluadora = "Camila Demo",
                evaluadoraClave = "camila demo",
                puntajeTotal = 80.0,
                observacionGeneral = "Obs",
                fecha = 500L,
                puntajes = listOf(PuntajeDto(1L, 4, "Bien"))
            )
        )
        coEvery { candidataDao.obtenerPorId(10L) } returns candidataRemotaTest(10L)
        coEvery { evaluacionDao.obtenerPorCandidata(10L) } returns null
        coEvery { evaluacionDao.insertar(any()) } returns 200L

        repository.sincronizarDesdeServidor()
        coVerify(exactly = 1) { evaluacionDao.insertarPuntajes(any()) }
        coVerify(exactly = 1) { candidataDao.actualizarEstado(10L, EstadoCandidatura.EVALUADA) }
    }

    @Test
    fun consultasYNormalizacionDeleganEnDaos() = runTest {
        coEvery { criterioDao.contar() } returns 0
        repository.asegurarCriterios(criterios)
        coVerify(exactly = 1) { criterioDao.insertarTodos(criterios) }

        coEvery { evaluacionDao.contarPorEvaluadora(10L, "camila demo") } returns 1
        assertTrue(repository.yaEvaluo(10L, "  CAMILA   Demo "))

        every { criterioDao.observarTodos() } returns flowOf(criterios)
        every { evaluacionDao.observarTodas() } returns flowOf(emptyList())
        every { evaluacionDao.observarPorCandidata(10L) } returns flowOf(emptyList())
        every { evaluacionDao.observarConsolidado() } returns flowOf(emptyList())
        every { evaluacionDao.observarFichas() } returns flowOf(emptyList())

        assertEquals(criterios, repository.observarCriterios().first())
        assertTrue(repository.observarTodas().first().isEmpty())
        assertTrue(repository.observarPorCandidata(10L).first().isEmpty())
        assertTrue(repository.observarConsolidado().first().isEmpty())
        assertTrue(repository.observarFichas().first().isEmpty())
    }
}