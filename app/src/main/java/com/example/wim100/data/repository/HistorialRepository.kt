package com.example.wim100.data.repository

import com.example.wim100.data.local.dao.HistorialDao
import com.example.wim100.model.RegistroHistorial
import kotlinx.coroutines.flow.Flow

class HistorialRepository(private val dao: HistorialDao) {

    fun observarTodo(): Flow<List<RegistroHistorial>> = dao.observarTodo()

    fun observarPorCandidata(candidataId: Long): Flow<List<RegistroHistorial>> {
        return dao.observarPorCandidata(candidataId)
    }

    suspend fun registrar(candidataId: Long?, accion: String, detalle: String, usuario: String) {
        val registro = RegistroHistorial(
            candidataId = candidataId,
            accion = accion,
            detalle = detalle,
            usuario = usuario
        )
        dao.insertar(registro)
    }
}