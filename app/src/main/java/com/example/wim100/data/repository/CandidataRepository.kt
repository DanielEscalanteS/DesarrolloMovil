package com.example.wim100.data.repository

import com.example.wim100.data.local.dao.CandidataDao
import com.example.wim100.model.Acciones
import com.example.wim100.model.Candidata
import com.example.wim100.model.EstadoCandidatura
import kotlinx.coroutines.flow.Flow

class CandidataRepository(
    private val dao: CandidataDao,
    private val historial: HistorialRepository
) {
    fun observarTodas(): Flow<List<Candidata>> = dao.observarTodas()

    fun observarPorId(id: Long): Flow<Candidata?> = dao.observarPorId(id)

    suspend fun obtener(id: Long): Candidata? = dao.obtenerPorId(id)

    suspend fun contar(): Int = dao.contar()

    suspend fun existeCorreo(correo: String): Boolean {
        return dao.contarPorCorreo(correo.trim()) > 0
    }

    suspend fun registrar(candidata: Candidata): Long {
        val idGenerado = dao.insertar(candidata)

        // Avisamos al historial que alguien registró una candidata
        historial.registrar(
            candidataId = idGenerado,
            accion = Acciones.REGISTRO,
            detalle = "Se registró a ${candidata.nombre}",
            usuario = candidata.registradaPor
        )
        return idGenerado
    }

    suspend fun resolver(id: Long, estado: EstadoCandidatura, usuario: String) {
        dao.actualizarEstado(id, estado)
        historial.registrar(
            candidataId = id,
            accion = Acciones.RESOLUCION,
            detalle = "Candidatura marcada como ${estado.etiqueta}",
            usuario = usuario
        )
    }
}