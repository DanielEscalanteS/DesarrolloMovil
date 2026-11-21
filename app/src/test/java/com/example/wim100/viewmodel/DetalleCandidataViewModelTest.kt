package com.example.wim100.viewmodel

import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.HistorialRepository
import com.example.wim100.model.*
import io.mockk.*
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleCandidataViewModelTest {
    private val repo = mockk<CandidataRepository>()
    private val evaluadora = Sesion("Camila", Rol.EVALUADORA)
    @Before fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repo.observarPorId(10) } returns flowOf(candidataRemotaTest())
        coEvery { repo.obtener(10) } returns candidataRemotaTest()
        coEvery { repo.eliminarEnServidor(10) } just Runs
    }
    @After fun terminar() { Dispatchers.resetMain() }
    @Test fun `consulta la candidata solicitada`() = runTest {
        val vm = DetalleCandidataViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.cargar(10)
        assertEquals(10L, vm.uiState.value.candidata?.id)
    }
    @Test fun `elimina en el servidor antes de indicar exito`() = runTest {
        val vm = DetalleCandidataViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.cargar(10); vm.eliminar(evaluadora)
        assertTrue(vm.uiState.value.eliminada)
        coVerify(exactly = 1) { repo.eliminarEnServidor(10) }
    }
    @Test fun `direccion no puede eliminar aunque invoque la accion`() = runTest {
        val vm = DetalleCandidataViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.cargar(10); vm.eliminar(Sesion("Dirección", Rol.DIRECCION))
        assertFalse(vm.uiState.value.eliminada)
        assertNotNull(vm.uiState.value.error)
        coVerify(exactly = 0) { repo.eliminarEnServidor(any()) }
    }
    @Test fun `candidatura resuelta no se elimina`() = runTest {
        coEvery { repo.obtener(10) } returns candidataRemotaTest().copy(estado = EstadoCandidatura.SELECCIONADA)
        val vm = DetalleCandidataViewModel(repo)
        vm.cargar(10); vm.eliminar(evaluadora)
        coVerify(exactly = 0) { repo.eliminarEnServidor(any()) }
    }
    @Test fun `fallo de red no indica eliminacion y permite reintentar`() = runTest {
        coEvery { repo.eliminarEnServidor(10) } throws IOException()
        val vm = DetalleCandidataViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.cargar(10); vm.eliminar(evaluadora)
        assertFalse(vm.uiState.value.eliminada)
        assertNotNull(vm.uiState.value.error)
        coEvery { repo.eliminarEnServidor(10) } just Runs
        vm.eliminar(evaluadora)
        assertTrue(vm.uiState.value.eliminada)
    }
    @Test fun `eliminar conserva un registro con nombre y responsable`() = runTest {
        val historial = mockk<HistorialRepository>(relaxed = true)
        val vm = DetalleCandidataViewModel(repo, historial)
        vm.cargar(10); vm.eliminar(evaluadora)
        coVerify { historial.registrar(null, "Eliminación de candidatura", match { it.contains("Ana Demo") }, "Camila") }
    }

}
