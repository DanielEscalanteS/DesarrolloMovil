package com.example.wim100.viewmodel

import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.EvaluacionRepository
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
class CandidatasViewModelTest {
    private val candidatas = mockk<CandidataRepository>()
    private val evaluaciones = mockk<EvaluacionRepository>(relaxed = true)
    @Before fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { candidatas.observarTodas() } returns flowOf(listOf(candidataRemotaTest()))
        every { evaluaciones.observarTodas() } returns flowOf(emptyList())
        coEvery { candidatas.sincronizarDesdeServidor() } just Runs
    }
    @After fun terminar() { Dispatchers.resetMain() }
    @Test fun `al entrar consulta servidor y conserva observacion de Room`() = runTest {
        val vm = CandidatasViewModel(candidatas, evaluaciones)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        assertEquals(10L, vm.uiState.value.candidatas.single().id)
        coVerify(exactly = 1) { candidatas.sincronizarDesdeServidor() }
    }
    @Test fun `fallo del refresco conserva la lista local y muestra error`() = runTest {
        coEvery { candidatas.sincronizarDesdeServidor() } throws IOException()
        val vm = CandidatasViewModel(candidatas, evaluaciones)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        assertEquals(1, vm.uiState.value.candidatas.size)
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.refrescando)
        coEvery { candidatas.sincronizarDesdeServidor() } just Runs
        vm.refrescar()
        assertNull(vm.uiState.value.error)
    }
    @Test fun `no duplica refrescos simultaneos`() = runTest {
        coEvery { candidatas.sincronizarDesdeServidor() } coAnswers { delay(100) }
        val vm = CandidatasViewModel(candidatas, evaluaciones)
        vm.refrescar(); vm.refrescar(); advanceUntilIdle()
        coVerify(exactly = 1) { candidatas.sincronizarDesdeServidor() }
    }
}
