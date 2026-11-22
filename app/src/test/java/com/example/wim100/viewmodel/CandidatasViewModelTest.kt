package com.example.wim100.viewmodel

import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.EvaluacionRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CandidatasViewModelTest {

    private val candidatas = mockk<CandidataRepository>()
    private val evaluaciones = mockk<EvaluacionRepository>(relaxed = true)

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { candidatas.observarTodas() } returns flowOf(listOf(candidataRemotaTest()))
        every { evaluaciones.observarTodas() } returns flowOf(emptyList())
        coEvery { candidatas.sincronizarDesdeServidor() } just Runs
    }

    @After
    fun terminar() {
        Dispatchers.resetMain()
    }

    @Test
    fun alEntrarConsultaServidorYConservaObservacionLocal() = runTest {
        val vm = CandidatasViewModel(candidatas, evaluaciones)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect()
        }

        assertEquals(10L, vm.uiState.value.candidatas.single().id)
        coVerify(exactly = 1) { candidatas.sincronizarDesdeServidor() }
    }

    @Test
    fun falloDelRefrescoConservaListaLocalYMuestraError() = runTest {
        coEvery { candidatas.sincronizarDesdeServidor() } throws IOException()
        val vm = CandidatasViewModel(candidatas, evaluaciones)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect()
        }

        assertEquals(1, vm.uiState.value.candidatas.size)
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.refrescando)

        coEvery { candidatas.sincronizarDesdeServidor() } just Runs
        vm.refrescar()

        assertNull(vm.uiState.value.error)
    }

    @Test
    fun noDuplicaRefrescosSimultaneos() = runTest {
        coEvery { candidatas.sincronizarDesdeServidor() } coAnswers { delay(100) }
        val vm = CandidatasViewModel(candidatas, evaluaciones)

        vm.refrescar()
        vm.refrescar()
        advanceUntilIdle()

        coVerify(exactly = 1) { candidatas.sincronizarDesdeServidor() }
    }
}