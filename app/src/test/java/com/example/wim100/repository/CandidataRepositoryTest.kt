package com.example.wim100.data.repository

import com.example.wim100.data.local.dao.CandidataDao
import com.example.wim100.data.remote.EstadoRequest
import com.example.wim100.data.remote.PostulanteDto
import com.example.wim100.data.remote.WimApi
import com.example.wim100.model.EstadoCandidatura
import com.example.wim100.model.Rol
import com.example.wim100.model.Sesion
import com.example.wim100.viewmodel.candidataRemotaTest
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CandidataRepositoryTest {

    private val dao = mockk<CandidataDao>(relaxed = true)
    private val historial = mockk<HistorialRepository>(relaxed = true)
    private val api = mockk<WimApi>()
    private lateinit var repository: CandidataRepository

    @Before
    fun preparar() {
        every { dao.observarTodas() } returns flowOf(emptyList())
        repository = CandidataRepository(dao, historial, api)
    }

    private fun dtoEjemplo(id: Long = 10L, estado: String = "POSTULADA") = PostulanteDto(
        id = id,
        nombre = "Ana Demo",
        correo = "ana@example.com",
        telefono = "+56 9 1111 2222",
        area = "Seguridad defensiva (Blue Team)",
        aniosExperiencia = 5,
        resumenExperiencia = "Experiencia ficticia de prueba.",
        linkedinUrl = "https://www.linkedin.com/in/ana-demo",
        cvNombre = "cv.pdf",
        estado = estado,
        fechaRegistro = 123L,
        registradaPor = "Camila"
    )

    @Test
    fun sincronizarDesdeServidorGuardaPostulantesYConservaCvUri() = runTest {
        val localConCv = candidataRemotaTest(id = 10L).copy(cvUri = "file:///local.pdf")
        every { dao.observarTodas() } returns flowOf(listOf(localConCv))
        coEvery { api.listarPostulantes() } returns listOf(
            dtoEjemplo(10L),
            dtoEjemplo(11L).copy(correo = "otra@example.com")
        )

        repository.sincronizarDesdeServidor()

        coVerify(exactly = 1) {
            dao.guardarDesdeServidor(match { it.id == 10L && it.cvUri == "file:///local.pdf" })
        }
        coVerify(exactly = 1) {
            dao.guardarDesdeServidor(match { it.id == 11L && it.cvUri == null })
        }
    }

    @Test
    fun sincronizarDesdeServidorDetectaConflictoDeIdOCorreo() = runTest {
        every { dao.observarTodas() } returns flowOf(listOf(candidataRemotaTest(id = 10L)))
        coEvery { api.listarPostulantes() } returns listOf(
            dtoEjemplo(10L).copy(correo = "distinto@example.com")
        )

        val fallo = runCatching { repository.sincronizarDesdeServidor() }
        assertTrue(fallo.isFailure)
    }

    @Test
    fun registrarLocalInsertaConHistorialYRechazaCorreoDuplicado() = runTest {
        val candidata = candidataRemotaTest(id = 0L)
        coEvery { dao.contarPorCorreo("ana@example.com") } returns 0
        coEvery { dao.insertarConHistorial(any(), any()) } returns 7L

        val id = repository.registrar(candidata)
        assertEquals(7L, id)

        coEvery { dao.contarPorCorreo("ana@example.com") } returns 1
        assertTrue(runCatching { repository.registrar(candidata) }.isFailure)
    }

    @Test
    fun resolverValidaRolYEstadoAntesDeGuardar() = runTest {
        val direccion = Sesion("Directora", Rol.DIRECCION)
        val evaluadora = Sesion("Camila", Rol.EVALUADORA)

        assertTrue(
            runCatching {
                repository.resolver(10L, EstadoCandidatura.SELECCIONADA, evaluadora)
            }.isFailure
        )
        assertTrue(
            runCatching {
                repository.resolver(10L, EstadoCandidatura.POSTULADA, direccion)
            }.isFailure
        )

        repository.resolver(10L, EstadoCandidatura.SELECCIONADA, direccion)
        coVerify(exactly = 1) {
            dao.resolverConHistorial(10L, EstadoCandidatura.SELECCIONADA, any(), any())
        }
    }

    @Test
    fun registrarYActualizarEnServidorSincronizanConRoom() = runTest {
        val local = candidataRemotaTest(id = 0L)
        coEvery { api.crearPostulante(any()) } returns dtoEjemplo(id = 55L)

        val idGenerado = repository.registrarEnServidor(local)
        assertEquals(55L, idGenerado)

        val editada = candidataRemotaTest(id = 10L).copy(nombre = "Ana Editada", cvUri = null)
        coEvery { dao.obtenerPorId(10L) } returns candidataRemotaTest(id = 10L).copy(cvUri = "file:///cv.pdf")
        coEvery { api.actualizarPostulante(10L, any()) } returns dtoEjemplo(id = 10L).copy(nombre = "Ana Editada")

        repository.actualizarEnServidor(editada)
        coVerify(exactly = 1) {
            dao.guardarDesdeServidor(match { it.nombre == "Ana Editada" && it.cvUri == "file:///cv.pdf" })
        }
    }

    @Test
    fun cambiarEstadoYEliminarEnServidorValidaIdYActualizaRoom() = runTest {
        assertTrue(runCatching { repository.eliminarEnServidor(0L) }.isFailure)
        assertTrue(
            runCatching {
                repository.cambiarEstadoEnServidor(0L, EstadoCandidatura.EVALUADA)
            }.isFailure
        )

        coEvery { api.cambiarEstado(10L, EstadoRequest("EVALUADA")) } returns dtoEjemplo(10L, "EVALUADA")
        repository.cambiarEstadoEnServidor(10L, EstadoCandidatura.EVALUADA)
        coVerify(exactly = 1) { dao.guardarDesdeServidor(match { it.estado == EstadoCandidatura.EVALUADA }) }

        coEvery { api.eliminarPostulante(10L) } just Runs
        repository.eliminarEnServidor(10L)
        coVerify(exactly = 1) { api.eliminarPostulante(10L) }
        coVerify(exactly = 1) { dao.eliminar(10L) }
    }

    @Test
    fun observarTodasYPorIdDeleganEnDao() = runTest {
        val lista = listOf(candidataRemotaTest(10L))
        every { dao.observarTodas() } returns flowOf(lista)
        every { dao.observarPorId(10L) } returns flowOf(lista.first())
        coEvery { dao.obtenerPorId(10L) } returns lista.first()
        coEvery { dao.contar() } returns 1

        assertEquals(lista, repository.observarTodas().first())
        assertEquals(lista.first(), repository.observarPorId(10L).first())
        assertEquals(lista.first(), repository.obtener(10L))
        assertEquals(1, repository.contar())
    }
}