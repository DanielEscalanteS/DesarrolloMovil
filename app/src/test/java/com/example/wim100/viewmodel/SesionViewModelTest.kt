package com.example.wim100.viewmodel

import com.example.wim100.data.repository.AuthRepository
import com.example.wim100.data.repository.HistorialRepository
import com.example.wim100.model.Acciones
import com.example.wim100.model.Rol
import com.example.wim100.model.Sesion
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = SesionViewModel(historial, auth)
    }

    @After
    fun terminar() {
        Dispatchers.resetMain()
    }

    private fun credenciales() {
        vm.onUsuario("evaluadora1")
        vm.onPassword("wim2026")
    }

    @Test
    fun camposVaciosNoConsultanApi() = runTest {
        vm.iniciarSesion()

        assertNotNull(vm.loginState.value.errorUsuario)
        assertNotNull(vm.loginState.value.errorPassword)
        coVerify(exactly = 0) { auth.iniciarSesion(any(), any()) }
    }

    @Test
    fun corregirCamposRetiraErrores() {
        vm.iniciarSesion()
        credenciales()

        assertNull(vm.loginState.value.errorUsuario)
        assertNull(vm.loginState.value.errorPassword)
    }

    @Test
    fun loginUtilizaIdentidadYRolDelServidor() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } returns evaluadora
        credenciales()
        vm.iniciarSesion()

        assertEquals(evaluadora, vm.sesion.value)
        assertEquals("", vm.loginState.value.password)
        coVerify { historial.registrar(null, Acciones.INICIO_SESION, any(), "Camila Demo") }
    }

    @Test
    fun direccionEsperaBiometriaAntesDeAbrirSesion() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } returns direccion
        credenciales()
        vm.iniciarSesion()

        assertNull(vm.sesion.value)
        assertTrue(vm.loginState.value.requiereBiometria)
        coVerify(exactly = 0) { historial.registrar(any(), any(), any(), any()) }

        vm.confirmarBiometria()

        assertEquals(direccion, vm.sesion.value)
    }

    @Test
    fun cancelarBiometriaImpideConfirmarPosteriormente() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } returns direccion
        credenciales()
        vm.iniciarSesion()
        vm.cancelarBiometria("Cancelada")
        vm.confirmarBiometria()

        assertNull(vm.sesion.value)
        assertFalse(vm.loginState.value.cargando)
        assertEquals("Cancelada", vm.loginState.value.error)
    }

    @Test
    fun error401PermiteCorregirClaveYReintentar() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } throws HttpException(
            Response.error<Any>(401, "".toResponseBody())
        )
        credenciales()
        vm.iniciarSesion()

        assertNull(vm.sesion.value)
        assertEquals("Usuario o contraseña incorrectos", vm.loginState.value.error)

        coEvery { auth.iniciarSesion(any(), any()) } returns evaluadora
        vm.onPassword("otra")
        vm.iniciarSesion()

        assertEquals(evaluadora, vm.sesion.value)
    }

    @Test
    fun falloDeRedMantieneUsuarioYPermiteReintentar() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } throws IOException()
        credenciales()
        vm.iniciarSesion()

        assertEquals("evaluadora1", vm.loginState.value.usuario)
        assertNotNull(vm.loginState.value.error)
        assertFalse(vm.loginState.value.cargando)
    }

    @Test
    fun dobleToqueSoloHaceUnaPeticion() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } coAnswers {
            delay(100)
            evaluadora
        }
        credenciales()
        vm.iniciarSesion()
        vm.iniciarSesion()
        advanceUntilIdle()

        coVerify(exactly = 1) { auth.iniciarSesion(any(), any()) }
    }

    @Test
    fun cerrarCancelaRespuestaTardiaDelLogin() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } coAnswers {
            delay(100)
            evaluadora
        }
        credenciales()
        vm.iniciarSesion()
        vm.cerrar()
        advanceUntilIdle()

        assertNull(vm.sesion.value)
        assertEquals("", vm.loginState.value.password)
    }

    @Test
    fun falloDelHistorialNoConcedeSesionIncompleta() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } returns evaluadora
        coEvery { historial.registrar(any(), any(), any(), any()) } throws IllegalStateException()
        credenciales()
        vm.iniciarSesion()

        assertNull(vm.sesion.value)
        assertNotNull(vm.loginState.value.error)
    }

    @Test
    fun cerrarEliminaSesionYRegistraSalida() = runTest {
        coEvery { auth.iniciarSesion(any(), any()) } returns evaluadora
        credenciales()
        vm.iniciarSesion()
        vm.cerrar()

        assertNull(vm.sesion.value)
        coVerify { historial.registrar(null, Acciones.CIERRE_SESION, any(), "Camila Demo") }
    }
}