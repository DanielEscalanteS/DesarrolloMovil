package com.example.wim100.viewmodel

import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.HistorialRepository
import com.example.wim100.model.EstadoCandidatura
import com.example.wim100.model.Rol
import com.example.wim100.model.Sesion
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleCandidataViewModelTest {

    private val repo = mockk<CandidataRepository>()
    private val evaluadora = Sesion("Camila", Rol.EVALUADORA)

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repo.observarPorId(10) } returns flowOf(candidataRemotaTest())
        coEvery { repo.obtener(10) } returns candidataRemotaTest()
        coEvery { repo.eliminarEnServidor(10) } just Runs
    }

    @After
    fun terminar() {
        Dispatchers.resetMain()
    }

    @Test
    fun consultaCandidataSolicitada() = runTest {
        val vm = DetalleCandidataViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect()
        }

        vm.cargar(10)

        assertEquals(10L, vm.uiState.value.candidata?.id)
    }

    @Test
    fun eliminaEnServidorAntesDeIndicarExito() = runTest {
        val vm = DetalleCandidataViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect()
        }

        vm.cargar(10)
        vm.eliminar(evaluadora)

        assertTrue(vm.uiState.value.eliminada)
        coVerify(exactly = 1) { repo.eliminarEnServidor(10) }
    }

    @Test
    fun direccionNoPuedeEliminarAunqueInvoqueAccion() = runTest {
        val vm = DetalleCandidataViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect()
        }

        vm.cargar(10)
        vm.eliminar(Sesion("Dirección", Rol.DIRECCION))

        assertFalse(vm.uiState.value.eliminada)
        assertNotNull(vm.uiState.value.error)
        coVerify(exactly = 0) { repo.eliminarEnServidor(any()) }
    }

    @Test
    fun candidaturaResueltaNoSeElimina() = runTest {
        coEvery { repo.obtener(10) } returns candidataRemotaTest().copy(
            estado = EstadoCandidatura.SELECCIONADA
        )
        val vm = DetalleCandidataViewModel(repo)

        vm.cargar(10)
        vm.eliminar(evaluadora)

        coVerify(exactly = 0) { repo.eliminarEnServidor(any()) }
    }

    @Test
    fun falloDeRedNoIndicaEliminacionYPermiteReintentar() = runTest {
        coEvery { repo.eliminarEnServidor(10) } throws IOException()
        val vm = DetalleCandidataViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect()
        }

        vm.cargar(10)
        vm.eliminar(evaluadora)

        assertFalse(vm.uiState.value.eliminada)
        assertNotNull(vm.uiState.value.error)

        coEvery { repo.eliminarEnServidor(10) } just Runs
        vm.eliminar(evaluadora)

        assertTrue(vm.uiState.value.eliminada)
    }

    @Test
    fun eliminarConservaRegistroConNombreYResponsable() = runTest {
        val historial = mockk<HistorialRepository>(relaxed = true)
        val vm = DetalleCandidataViewModel(repo, historial)

        vm.cargar(10)
        vm.eliminar(evaluadora)

        coVerify {
            historial.registrar(
                null,
                "Eliminación de candidatura",
                match { it.contains("Ana Demo") },
                "Camila"
            )
        }
    }
}