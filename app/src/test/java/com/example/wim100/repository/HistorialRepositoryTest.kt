package com.example.wim100.data.repository

import com.example.wim100.data.local.dao.HistorialDao
import com.example.wim100.model.RegistroHistorial
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HistorialRepositoryTest {

    private val dao = mockk<HistorialDao>(relaxed = true)
    private lateinit var repository: HistorialRepository

    @Before
    fun preparar() {
        repository = HistorialRepository(dao)
    }

    @Test
    fun registrarInsertaEventoConDatosCompletos() = runTest {
        repository.registrar(10L, "Registro", "Postulación ingresada", "Camila")

        coVerify(exactly = 1) {
            dao.insertar(
                match {
                    it.candidataId == 10L &&
                            it.accion == "Registro" &&
                            it.detalle == "Postulación ingresada" &&
                            it.usuario == "Camila"
                }
            )
        }
    }

    @Test
    fun observarTodoYPorCandidataDeleganEnDao() = runTest {
        val registro = RegistroHistorial(
            id = 1L,
            candidataId = 10L,
            accion = "Registro",
            detalle = "Detalle",
            usuario = "Camila"
        )
        every { dao.observarTodo() } returns flowOf(listOf(registro))
        every { dao.observarPorCandidata(10L) } returns flowOf(listOf(registro))

        assertEquals(listOf(registro), repository.observarTodo().first())
        assertEquals(listOf(registro), repository.observarPorCandidata(10L).first())
    }
}