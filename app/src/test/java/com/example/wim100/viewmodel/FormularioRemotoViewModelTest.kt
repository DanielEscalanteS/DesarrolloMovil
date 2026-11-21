package com.example.wim100.viewmodel

import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.RandomUserRepository
import com.example.wim100.data.repository.HistorialRepository
import com.example.wim100.model.*
import io.mockk.*
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

internal fun candidataRemotaTest(id: Long = 10) = Candidata(
    id = id, nombre = "Ana Demo", correo = "ana@example.com", telefono = "+56 9 1111 2222",
    area = AREAS_ESPECIALIDAD.first(), aniosExperiencia = 5,
    resumenExperiencia = "Experiencia ficticia suficientemente larga para validar el formulario.",
    linkedinUrl = "https://www.linkedin.com/in/ana-demo", cvUri = "file:///cv.pdf",
    cvNombre = "cv.pdf", registradaPor = "Evaluadora Original", fechaRegistro = 123L)

@OptIn(ExperimentalCoroutinesApi::class)
class FormularioRemotoViewModelTest {
    private val repository = mockk<CandidataRepository>()
    private val randomUser = mockk<RandomUserRepository>()
    private lateinit var vm: FormularioViewModel
    @Before fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = FormularioViewModel(repository, randomUser)
    }
    @After fun terminar() { Dispatchers.resetMain() }
    private fun formulario() {
        val c = candidataRemotaTest()
        vm.actualizarNombre(c.nombre); vm.actualizarCorreo(c.correo); vm.actualizarTelefono(c.telefono)
        vm.actualizarArea(c.area); vm.actualizarAnios("5"); vm.actualizarResumen(c.resumenExperiencia)
        vm.actualizarLinkedin(c.linkedinUrl); vm.actualizarCv(c.cvUri!!, c.cvNombre, 100L)
    }
    @Test fun `crear envia al servidor y utiliza su id`() = runTest {
        coEvery { repository.registrarEnServidor(any()) } returns 42L
        formulario(); vm.enviarPostulacion("Camila Demo")
        assertEquals(42L, vm.uiState.value.idGuardado)
        coVerify { repository.registrarEnServidor(match { it.id == 0L && it.registradaPor == "Camila Demo" }) }
        coVerify(exactly = 0) { repository.registrar(any()) }
    }
    @Test fun `postulacion publica registra el nombre de la postulante sin cuenta`() = runTest {
        val historial = mockk<HistorialRepository>()
        coEvery { historial.registrar(any(), any(), any(), any()) } just Runs
        coEvery { repository.registrarEnServidor(any()) } returns 45L
        vm = FormularioViewModel(repository, randomUser, historial)
        formulario(); vm.actualizarNombre("  Ana Demo  "); vm.enviarPostulacion()
        assertEquals(45L, vm.uiState.value.idGuardado)
        coVerify(exactly = 1) { repository.registrarEnServidor(match {
            it.registradaPor == "Ana Demo" && it.estado == EstadoCandidatura.POSTULADA
        }) }
        coVerify { historial.registrar(45L, Acciones.REGISTRO, any(), "Ana Demo") }
    }
    @Test fun `postulacion publica invalida no hace peticiones`() = runTest {
        vm.enviarPostulacion()
        coVerify(exactly = 0) { repository.registrarEnServidor(any()) }
        assertTrue(vm.uiState.value.errores.hayErrores)
        assertNull(vm.uiState.value.idGuardado)
    }
    @Test fun `un formulario invalido no hace peticiones`() = runTest {
        vm.enviarPostulacion("Camila")
        coVerify(exactly = 0) { repository.registrarEnServidor(any()) }
        assertTrue(vm.uiState.value.errores.hayErrores)
    }
    @Test fun `editar precarga y conserva id fecha responsable y cv`() = runTest {
        val c = candidataRemotaTest()
        coEvery { repository.obtener(10) } returns c
        coEvery { repository.actualizarEnServidor(any()) } just Runs
        vm.cargar(10); vm.actualizarNombre("Ana Actualizada"); vm.enviarPostulacion("Otra Evaluadora")
        coVerify { repository.actualizarEnServidor(match {
            it.id == 10L && it.fechaRegistro == 123L && it.registradaPor == c.registradaPor &&
                it.cvUri == c.cvUri && it.nombre == "Ana Actualizada"
        }) }
        coVerify(exactly = 0) { repository.registrarEnServidor(any()) }
        assertEquals(10L, vm.uiState.value.idGuardado)
    }
    @Test fun `editar un ejemplo permite conservar el cv que no esta en este telefono`() = runTest {
        coEvery { repository.obtener(10) } returns candidataRemotaTest().copy(cvUri = null)
        coEvery { repository.actualizarEnServidor(any()) } just Runs
        vm.cargar(10); vm.enviarPostulacion("Camila")
        assertEquals(10L, vm.uiState.value.idGuardado)
    }
    @Test fun `no se permite cargar para editar una candidatura resuelta`() = runTest {
        coEvery { repository.obtener(10) } returns candidataRemotaTest().copy(estado = EstadoCandidatura.SELECCIONADA)
        vm.cargar(10)
        assertFalse(vm.uiState.value.esEdicion)
        assertNotNull(vm.uiState.value.errorGuardado)
    }
    @Test fun `correo duplicado se muestra en el campo y permite corregirlo`() = runTest {
        coEvery { repository.registrarEnServidor(any()) } throws HttpException(Response.error<Any>(409, "".toResponseBody()))
        formulario(); vm.enviarPostulacion("Camila")
        assertNotNull(vm.uiState.value.errores.correo)
        vm.actualizarCorreo("otra@example.com")
        assertNull(vm.uiState.value.errores.correo)
        coEvery { repository.registrarEnServidor(any()) } returns 43L
        vm.enviarPostulacion("Camila")
        assertEquals(43L, vm.uiState.value.idGuardado)
    }
    @Test fun `fallo de red conserva los datos escritos`() = runTest {
        coEvery { repository.registrarEnServidor(any()) } throws IOException()
        formulario(); vm.enviarPostulacion("Camila")
        assertEquals("Ana Demo", vm.uiState.value.nombre)
        assertNull(vm.uiState.value.idGuardado)
        assertFalse(vm.uiState.value.guardando)
    }
    @Test fun `doble envio no duplica la postulacion`() = runTest {
        coEvery { repository.registrarEnServidor(any()) } coAnswers { delay(100); 44L }
        formulario(); vm.enviarPostulacion("Camila"); vm.enviarPostulacion("Camila")
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.registrarEnServidor(any()) }
    }
    @Test fun `random user solo modifica el nombre`() = runTest {
        coEvery { randomUser.obtenerNombreAleatorio() } returns "Lucía Ejemplo"
        formulario(); vm.generarNombreFicticio()
        assertEquals("Lucía Ejemplo", vm.uiState.value.nombre)
        assertEquals("ana@example.com", vm.uiState.value.correo)
        coVerify(exactly = 0) { repository.registrarEnServidor(any()) }
    }
    @Test fun `fallo de random user permite seguir editando`() = runTest {
        coEvery { randomUser.obtenerNombreAleatorio() } throws IOException()
        formulario(); vm.generarNombreFicticio(); vm.actualizarNombre("Nombre Manual")
        assertEquals("Nombre Manual", vm.uiState.value.nombre)
        assertFalse(vm.uiState.value.generandoNombre)
    }
    @Test fun `cargar de nuevo el mismo id no borra cambios sin guardar`() = runTest {
        coEvery { repository.obtener(10) } returns candidataRemotaTest()
        vm.cargar(10); vm.actualizarNombre("Cambio pendiente"); vm.cargar(10)
        assertEquals("Cambio pendiente", vm.uiState.value.nombre)
        coVerify(exactly = 1) { repository.obtener(10) }
    }
    @Test fun `crear registra en el historial a la evaluadora autenticada`() = runTest {
        val historial = mockk<HistorialRepository>(relaxed = true)
        vm = FormularioViewModel(repository, randomUser, historial)
        coEvery { repository.registrarEnServidor(any()) } returns 42L
        formulario(); vm.enviarPostulacion("Camila")
        coVerify { historial.registrar(42, Acciones.REGISTRO, any(), "Camila") }
    }
    @Test fun `fallo del historial no convierte una creacion exitosa en un reenvio`() = runTest {
        val historial = mockk<HistorialRepository>()
        coEvery { historial.registrar(any(), any(), any(), any()) } throws IOException()
        vm = FormularioViewModel(repository, randomUser, historial)
        coEvery { repository.registrarEnServidor(any()) } returns 42L
        formulario(); vm.enviarPostulacion("Camila"); vm.enviarPostulacion("Camila")
        assertEquals(42L, vm.uiState.value.idGuardado)
        assertNotNull(vm.uiState.value.aviso)
        coVerify(exactly = 1) { repository.registrarEnServidor(any()) }
    }

}
