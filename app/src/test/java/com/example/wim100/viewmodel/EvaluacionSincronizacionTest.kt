package com.example.wim100.viewmodel

import com.example.wim100.data.local.CandidataConEvaluaciones
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.EvaluacionRepository
import com.example.wim100.model.CriteriosDemostracion
import com.example.wim100.model.EstadoCandidatura
import com.example.wim100.model.Rol
import com.example.wim100.model.Sesion
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class EvaluacionSincronizacionTest {

    private val repo = mockk<EvaluacionRepository>(relaxed = true)
    private val candidatas = mockk<CandidataRepository>()
    private val sesion = Sesion("Camila", Rol.EVALUADORA)
    private lateinit var vm: EvaluacionViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { repo.obtenerCriterios() } returns CriteriosDemostracion.lista
        coEvery { repo.obtenerFicha(10) } returns CandidataConEvaluaciones(
            candidataRemotaTest(),
            emptyList()
        )
        coEvery { candidatas.obtener(10) } returns candidataRemotaTest().copy(
            estado = EstadoCandidatura.EVALUADA
        )
        coEvery { candidatas.cambiarEstadoEnServidor(10, EstadoCandidatura.EVALUADA) } just Runs
        vm = EvaluacionViewModel(repo, candidatas)
    }

    @After
    fun terminar() {
        Dispatchers.resetMain()
    }

    private fun completar() {
        vm.cargarCriterios(10, sesion)
        CriteriosDemostracion.lista.forEach { vm.actualizarPuntaje(it.id, 5) }
        vm.actualizarObservacionGeneral("Cumple ampliamente con los criterios de esta evaluación.")
    }

    @Test
    fun guardaEvaluacionLocalYSincronizaEstado() = runTest {
        completar()
        vm.guardarEvaluacion(10, sesion)

        assertTrue(vm.uiState.value.finalizadoConExito)
        coVerify { candidatas.cambiarEstadoEnServidor(10, EstadoCandidatura.EVALUADA) }
    }

    @Test
    fun reintentarSincronizacionNoRegistraOtraEvaluacion() = runTest {
        coEvery { candidatas.cambiarEstadoEnServidor(10, any()) } throws IOException()
        completar()
        vm.guardarEvaluacion(10, sesion)

        assertTrue(vm.uiState.value.pendienteSincronizar)
        assertFalse(vm.uiState.value.finalizadoConExito)

        vm.actualizarPuntaje(CriteriosDemostracion.lista.first().id, 1)
        assertEquals(5, vm.uiState.value.puntajes[CriteriosDemostracion.lista.first().id])

        coEvery { candidatas.cambiarEstadoEnServidor(10, any()) } just Runs
        vm.guardarEvaluacion(10, sesion)

        assertTrue(vm.uiState.value.finalizadoConExito)
        coVerify(exactly = 1) { repo.registrar(any(), any(), any(), any(), any()) }
    }

    @Test
    fun direccionNoPuedeCargarEvaluacionParaModificarla() = runTest {
        vm.cargarCriterios(10, Sesion("Dirección", Rol.DIRECCION))

        assertFalse(vm.uiState.value.puedeGuardar)
        assertNotNull(vm.uiState.value.errorGeneral)
    }

    @Test
    fun editarEvaluacionCompletaSincronizaEstadoEvaluada() = runTest {
        coEvery { candidatas.obtener(10) } returns candidataRemotaTest()
        completar()
        vm.guardarEvaluacion(10, sesion)

        coVerify { candidatas.cambiarEstadoEnServidor(10, EstadoCandidatura.EVALUADA) }
        assertTrue(vm.uiState.value.finalizadoConExito)
    }
}