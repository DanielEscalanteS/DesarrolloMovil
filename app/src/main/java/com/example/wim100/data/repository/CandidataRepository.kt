package com.example.wim100.data.repository

import com.example.wim100.data.local.dao.CandidataDao
import android.database.sqlite.SQLiteConstraintException
import com.example.wim100.model.RegistroHistorial
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
        val normalizada = candidata.copy(correo = candidata.correo.trim().lowercase())
        require(!existeCorreo(normalizada.correo)) { "Ya existe una postulación con este correo" }
        return try {
            dao.insertarConHistorial(normalizada, RegistroHistorial(
                candidataId = null,
                accion = Acciones.REGISTRO,
                detalle = "Se registró a ${normalizada.nombre}",
                usuario = normalizada.registradaPor
            ))
        } catch (e: SQLiteConstraintException) {
            if (existeCorreo(normalizada.correo)) {
                throw IllegalArgumentException("Ya existe una postulación con este correo", e)
            }
            throw e
        }
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