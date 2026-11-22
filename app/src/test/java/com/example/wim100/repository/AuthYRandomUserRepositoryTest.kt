package com.example.wim100.data.repository

import com.example.wim100.data.remote.LoginRequest
import com.example.wim100.data.remote.LoginResponse
import com.example.wim100.data.remote.RandomUserApi
import com.example.wim100.data.remote.RandomUserNombre
import com.example.wim100.data.remote.RandomUserResponse
import com.example.wim100.data.remote.RandomUserResultado
import com.example.wim100.data.remote.WimApi
import com.example.wim100.model.Rol
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthYRandomUserRepositoryTest {

    @Test
    fun authRepositoryRetornaSesionConRolMapeado() = runTest {
        val api = mockk<WimApi>()
        coEvery { api.login(LoginRequest("evaluadora1", "wim2026")) } returns LoginResponse(
            id = 1L,
            usuario = "evaluadora1",
            nombre = "Camila Demo",
            rol = "EVALUADORA"
        )

        val sesion = AuthRepository(api).iniciarSesion("  evaluadora1 ", "wim2026")

        assertEquals("Camila Demo", sesion.nombre)
        assertEquals(Rol.EVALUADORA, sesion.rol)
        assertEquals(1L, sesion.id)
    }

    @Test
    fun randomUserRepositoryRetornaNombreCompletoOFallaSiEstaVacio() = runTest {
        val api = mockk<RandomUserApi>()
        val repo = RandomUserRepository(api)

        coEvery { api.obtenerNombreAleatorio() } returns RandomUserResponse(
            results = listOf(RandomUserResultado(RandomUserNombre("Lucía", "Pérez")))
        )
        assertEquals("Lucía Pérez", repo.obtenerNombreAleatorio())

        coEvery { api.obtenerNombreAleatorio() } returns RandomUserResponse(emptyList())
        assertTrue(runCatching { repo.obtenerNombreAleatorio() }.isFailure)
    }
}