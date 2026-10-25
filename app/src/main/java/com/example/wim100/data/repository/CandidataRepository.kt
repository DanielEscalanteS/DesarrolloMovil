package com.example.wim100.data.repository

import com.example.wim100.data.local.dao.CandidataDao
import android.database.sqlite.SQLiteConstraintException
import com.example.wim100.model.RegistroHistorial
import com.example.wim100.model.Acciones
import com.example.wim100.model.Sesion
import com.example.wim100.model.Rol
import com.example.wim100.model.Evaluacion
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
        try {
            // Avisa al historial que alguien registró una candidata
            val registro = RegistroHistorial(
                candidataId = null,
                accion = Acciones.REGISTRO,
                detalle = "Se registró a ${normalizada.nombre}",
                usuario = normalizada.registradaPor
            )
            val idGenerado = dao.insertarConHistorial(normalizada, registro)
            return idGenerado
        } catch (e: SQLiteConstraintException) {
            if (existeCorreo(normalizada.correo)) {
                throw IllegalArgumentException("Ya existe una postulación con este correo", e)
            }
            throw e
        }
    }

    suspend fun resolver(id: Long, estado: EstadoCandidatura, sesion: Sesion) {
        require(sesion.rol == Rol.DIRECCION) { "Solo Dirección puede resolver candidaturas" }
        require(estado.estaResuelta) { "Selecciona una resolución válida" }
        dao.resolverConHistorial(
            id, estado,
            RegistroHistorial(
                candidataId = id,
                accion = Acciones.RESOLUCION,
                detalle = "Candidatura marcada como ${estado.etiqueta}",
                usuario = sesion.nombre
            ),
            Evaluacion.MINIMO_PARA_CONSOLIDAR
        )
    }
}
