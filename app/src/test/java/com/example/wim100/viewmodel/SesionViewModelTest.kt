package com.example.wim100.viewmodel

import com.example.wim100.data.repository.AuthRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class SesionViewModelTest {
    private val auth = mockk<AuthRepository>()
    private val historial = mockk<HistorialRepository>(relaxed = true)
    private lateinit var vm: SesionViewModel
    private val evaluadora = Sesion("Camila Demo", Rol.EVALUADORA, 1, "evaluadora1")
    private val direccion = Sesion("Dirección Demo", Rol.DIRECCION, 3, "direccion")

    @Before fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = SesionViewModel(historial, auth)
    }
    @After fun terminar() { Dispatchers.resetMain() }
    private fun credenciales() { vm.onUsuario("evaluadora1"); vm.onPassword("wim2026") }

    @Test fun `campos vacios no consultan la API`() = runTest {
        vm.iniciarSesion()
        assertNotNull(vm.loginState.value.errorUsuario)
        assertNotNull(vm.loginState.value.errorPassword)
        coVerify(exactly = 0) { auth.iniciarSesion(any(), any()) }
    }
    @Test fun `corregir campos retira sus errores`() {
        vm.iniciarSesion(); credenciales()
        assertNull(vm.loginState.value.errorUsuario)
        assertNull(vm.loginState.value.errorPassword)
    }
    @Test fun `login utiliza la identidad y rol del servidor`() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } returns evaluadora
        credenciales(); vm.iniciarSesion()
        assertEquals(evaluadora, vm.sesion.value)
        assertEquals("", vm.loginState.value.password)
        coVerify { historial.registrar(null, Acciones.INICIO_SESION, any(), "Camila Demo") }
    }
    @Test fun `direccion espera la biometria antes de abrir sesion`() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } returns direccion
        credenciales(); vm.iniciarSesion()
        assertNull(vm.sesion.value)
        assertTrue(vm.loginState.value.requiereBiometria)
        coVerify(exactly = 0) { historial.registrar(any(), any(), any(), any()) }
        vm.confirmarBiometria()
        assertEquals(direccion, vm.sesion.value)
    }
    @Test fun `cancelar biometria impide confirmar posteriormente`() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } returns direccion
        credenciales(); vm.iniciarSesion(); vm.cancelarBiometria("Cancelada"); vm.confirmarBiometria()
        assertNull(vm.sesion.value)
        assertFalse(vm.loginState.value.cargando)
        assertEquals("Cancelada", vm.loginState.value.error)
    }
    @Test fun `un 401 permite corregir la clave y reintentar`() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } throws HttpException(Response.error<Any>(401, "".toResponseBody()))
        credenciales(); vm.iniciarSesion()
        assertNull(vm.sesion.value)
        assertEquals("Usuario o contraseña incorrectos", vm.loginState.value.error)
        coEvery { auth.iniciarSesion(any(), any()) } returns evaluadora
        vm.onPassword("otra"); vm.iniciarSesion()
        assertEquals(evaluadora, vm.sesion.value)
    }
    @Test fun `un fallo de red mantiene el usuario y permite reintentar`() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } throws IOException()
        credenciales(); vm.iniciarSesion()
        assertEquals("evaluadora1", vm.loginState.value.usuario)
        assertNotNull(vm.loginState.value.error)
        assertFalse(vm.loginState.value.cargando)
    }
    @Test fun `doble toque solo hace una peticion`() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } coAnswers { delay(100); evaluadora }
        credenciales(); vm.iniciarSesion(); vm.iniciarSesion()
        advanceUntilIdle()
        coVerify(exactly = 1) { auth.iniciarSesion(any(), any()) }
    }
    @Test fun `cerrar cancela una respuesta tardia del login`() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } coAnswers { delay(100); evaluadora }
        credenciales(); vm.iniciarSesion(); vm.cerrar(); advanceUntilIdle()
        assertNull(vm.sesion.value)
        assertEquals("", vm.loginState.value.password)
    }
    @Test fun `fallo del historial no concede una sesion incompleta`() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } returns evaluadora
        coEvery { historial.registrar(any(), any(), any(), any()) } throws IllegalStateException()
        credenciales(); vm.iniciarSesion()
        assertNull(vm.sesion.value)
        assertNotNull(vm.loginState.value.error)
    }
    @Test fun `cerrar elimina la sesion y registra la salida`() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } returns evaluadora
        credenciales(); vm.iniciarSesion(); vm.cerrar()
        assertNull(vm.sesion.value)
        coVerify { historial.registrar(null, Acciones.CIERRE_SESION, any(), "Camila Demo") }
    }
}
