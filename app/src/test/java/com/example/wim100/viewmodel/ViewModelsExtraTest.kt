package com.example.wim100.viewmodel

import com.example.wim100.data.local.CandidataConEvaluaciones
import com.example.wim100.data.local.EvaluacionConPuntajes
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.DatosEjemploRepository
import com.example.wim100.data.repository.EvaluacionRepository
import com.example.wim100.data.repository.HistorialRepository
import com.example.wim100.model.CriteriosDemostracion
import com.example.wim100.model.EstadoCandidatura
import com.example.wim100.model.Evaluacion
import com.example.wim100.model.RegistroHistorial
import com.example.wim100.model.ResultadoConsolidado
import io.mockk.Runs
import io.mockk.coEvery
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsExtraTest {

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun terminar() {
        Dispatchers.resetMain()
    }

    private fun fichaEvaluada(id: Long, area: String = "Seguridad defensiva (Blue Team)") =
        CandidataConEvaluaciones(
            candidata = candidataRemotaTest(id).copy(area = area, estado = EstadoCandidatura.EVALUADA),
            evaluaciones = listOf(
                EvaluacionConPuntajes(
                    Evaluacion(
                        id = id,
                        candidataId = id,
                        evaluadora = "Camila",
                        evaluadoraClave = "camila",
                        puntajeTotal = 85.0,
                        observacionGeneral = "Observación válida."
                    ),
                    emptyList()
                )
            )
        )

    @Test
    fun comparacionViewModelValidaSeleccionYCargaFichas() = runTest {
        val repo = mockk<EvaluacionRepository>()
        every { repo.observarFichas() } returns flowOf(listOf(fichaEvaluada(1L), fichaEvaluada(2L)))
        every { repo.observarCriterios() } returns flowOf(CriteriosDemostracion.lista)

        val vm = ComparacionViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        vm.cargar(listOf(1L, 2L))
        assertEquals(2, vm.uiState.value.candidatas.size)
        assertNull(vm.uiState.value.error)

        vm.cargar(listOf(1L, 99L))
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun detalleEvaluacionesViewModelCargaFichaOInformaSiNoExiste() = runTest {
        val repo = mockk<EvaluacionRepository>()
        every { repo.observarFicha(1L) } returns flowOf(fichaEvaluada(1L))
        every { repo.observarFicha(99L) } returns flowOf(null)
        every { repo.observarCriterios() } returns flowOf(CriteriosDemostracion.lista)

        val vm = DetalleEvaluacionesViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        vm.cargar(1L)
        assertEquals(1L, vm.uiState.value.ficha?.candidata?.id)
        assertNull(vm.uiState.value.error)

        vm.cargar(99L)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun historialYDatosEjemploViewModelManejanExitoYError() = runTest {
        val histRepo = mockk<HistorialRepository>()
        val reg = RegistroHistorial(1L, 10L, "Acción", "Detalle", "Camila")
        every { histRepo.observarTodo() } returns flowOf(listOf(reg))

        val histVm = HistorialViewModel(histRepo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { histVm.uiState.collect() }
        assertEquals(listOf(reg), histVm.uiState.value.registros)

        val ejRepo = mockk<DatosEjemploRepository>()
        coEvery { ejRepo.cargar() } throws IOException("Sin red")
        val ejVm = DatosEjemploViewModel(ejRepo)
        assertFalse(ejVm.uiState.value.cargando)
        assertNotNull(ejVm.uiState.value.error)

        coEvery { ejRepo.cargar() } just Runs
        ejVm.preparar()
        assertNull(ejVm.uiState.value.error)
    }

    @Test
    fun direccionViewModelFiltraYControlaSeleccionComparacion() = runTest {
        val evalRepo = mockk<EvaluacionRepository>()
        val candRepo = mockk<CandidataRepository>(relaxed = true)
        val histRepo = mockk<HistorialRepository>(relaxed = true)
        val f1 = fichaEvaluada(1L, "Blue Team")
        val f2 = fichaEvaluada(2L, "Blue Team")
        val f3 = fichaEvaluada(3L, "Red Team")

        every { evalRepo.observarCriterios() } returns flowOf(CriteriosDemostracion.lista)
        every { evalRepo.observarFichas() } returns flowOf(listOf(f1, f2, f3))
        every { evalRepo.observarConsolidado() } returns flowOf(
            listOf(
                ResultadoConsolidado(1L, "Ana", "Blue Team", EstadoCandidatura.EVALUADA, 1, 90.0),
                ResultadoConsolidado(3L, "Berta", "Red Team", EstadoCandidatura.EVALUADA, 1, 80.0)
            )
        )

        val vm = DireccionViewModel(evalRepo, candRepo, histRepo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.fichas.collect()
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.listado.collect()
        }

        vm.filtrar(FiltrosDireccion(area = "Blue Team"))
        assertEquals(listOf(1L), vm.listado.value.map { it.candidataId })

        vm.alternarComparacion(1L)
        vm.alternarComparacion(3L) // Área distinta: debe rechazar y avisar
        assertEquals(listOf(1L), vm.seleccionadas.value)
        assertNotNull(vm.mensaje.value)

        vm.alternarComparacion(2L)
        assertEquals(listOf(1L, 2L), vm.seleccionadas.value)

        vm.limpiarSeleccion()
        assertTrue(vm.seleccionadas.value.isEmpty())
    }
}