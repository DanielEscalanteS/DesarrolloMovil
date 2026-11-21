package com.example.wim100.viewmodel

import com.example.wim100.data.local.*
import com.example.wim100.data.repository.*
import com.example.wim100.model.*
import io.mockk.*
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DireccionResolucionTest {
    private val repo = mockk<EvaluacionRepository>()
    private val candidatas = mockk<CandidataRepository>()
    private val historial = mockk<HistorialRepository>(relaxed = true)
    private val sesion = Sesion("Dirección", Rol.DIRECCION)
    private lateinit var vm: DireccionViewModel
    @Before fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repo.observarCriterios() } returns flowOf(emptyList())
        every { repo.observarFichas() } returns flowOf(emptyList())
        every { repo.observarConsolidado() } returns flowOf(emptyList())
        coEvery { candidatas.cambiarEstadoEnServidor(any(), any()) } just Runs
        coEvery { repo.obtenerFicha(10) } returns CandidataConEvaluaciones(
            candidataRemotaTest().copy(estado = EstadoCandidatura.EVALUADA),
            listOf(EvaluacionConPuntajes(Evaluacion(candidataId = 10, evaluadora = "Camila",
                evaluadoraClave = "camila", puntajeTotal = 90.0, observacionGeneral = "Ejemplo"), emptyList())))
        coEvery { repo.asegurarCriterios(any()) } just Runs
        coEvery { candidatas.sincronizarDesdeServidor() } just Runs
        vm = DireccionViewModel(repo, candidatas, historial)
        vm.refrescar()
    }
    @After fun terminar() { Dispatchers.resetMain() }
    @Test fun `resuelve en el servidor y registra al responsable`() = runTest {
        vm.resolverCandidatura(10, EstadoCandidatura.SELECCIONADA, sesion)
        assertEquals("Resolución guardada", vm.mensaje.value)
        coVerify { candidatas.cambiarEstadoEnServidor(10, EstadoCandidatura.SELECCIONADA) }
        coVerify { historial.registrar(10, Acciones.RESOLUCION, any(), sesion.nombre) }
    }
    @Test fun `evaluadora no puede resolver`() = runTest {
        vm.resolverCandidatura(10, EstadoCandidatura.SELECCIONADA, Sesion("Camila", Rol.EVALUADORA))
        coVerify(exactly = 0) { candidatas.cambiarEstadoEnServidor(any(), any()) }
    }
    @Test fun `sin evaluacion no envia una resolucion`() = runTest {
        coEvery { repo.obtenerFicha(10) } returns CandidataConEvaluaciones(candidataRemotaTest(), emptyList())
        vm.resolverCandidatura(10, EstadoCandidatura.SELECCIONADA, sesion)
        coVerify(exactly = 0) { candidatas.cambiarEstadoEnServidor(any(), any()) }
    }
    @Test fun `fallo del servidor no registra un exito en el historial`() = runTest {
        coEvery { candidatas.cambiarEstadoEnServidor(any(), any()) } throws IOException()
        vm.resolverCandidatura(10, EstadoCandidatura.NO_SELECCIONADA, sesion)
        assertNotEquals("Resolución guardada", vm.mensaje.value)
        assertFalse(vm.guardando.value)
        coVerify(exactly = 0) { historial.registrar(any(), any(), any(), any()) }
    }
    @Test fun `un refresco fallido bloquea resoluciones sobre datos anteriores`() = runTest {
        coEvery { candidatas.sincronizarDesdeServidor() } throws IOException()
        vm.refrescar(); vm.resolverCandidatura(10, EstadoCandidatura.SELECCIONADA, sesion)
        coVerify(exactly = 0) { candidatas.cambiarEstadoEnServidor(any(), any()) }
        assertNotEquals("Resolución guardada", vm.mensaje.value)
    }

}
