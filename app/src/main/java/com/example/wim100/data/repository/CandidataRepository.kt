package com.example.wim100.data.repository

import com.example.wim100.data.remote.aPostulanteRequest
import com.example.wim100.data.remote.aCandidata
import com.example.wim100.data.remote.WimApi
import com.example.wim100.data.remote.RetrofitClient
import com.example.wim100.data.remote.EstadoRequest
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
import kotlinx.coroutines.flow.first

class CandidataRepository(
    private val dao: CandidataDao,
    private val historial: HistorialRepository,
    private val api: WimApi = RetrofitClient.wimApi
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


    suspend fun sincronizarDesdeServidor() {
        val postulantes = api.listarPostulantes()
        val candidatasLocales = dao.observarTodas().first()

        val localesPorId = candidatasLocales.associateBy { it.id }
        val localesPorCorreo = candidatasLocales.associateBy {
            it.correo.trim().lowercase()
        }

        postulantes.forEach { dto ->
            val localPorId = localesPorId[dto.id]

            check(
                localPorId == null ||
                        localPorId.correo.equals(dto.correo, ignoreCase = true)
            ) {
                "Conflicto de ID ${dto.id}: existe otra candidata local"
            }

            val localPorCorreo = localesPorCorreo[dto.correo.trim().lowercase()]

            check(
                localPorCorreo == null || localPorCorreo.id == dto.id
            ) {
                "Conflicto de correo: existe una candidata local con otro ID"
            }
        }

        postulantes.forEach { dto ->
            val candidataLocal = localesPorId[dto.id]

            dao.guardarDesdeServidor(
                dto.aCandidata(cvUri = candidataLocal?.cvUri)
            )
        }
    }

    suspend fun registrarEnServidor(candidata: Candidata): Long {
        val respuesta = api.crearPostulante(
            candidata.aPostulanteRequest()
        )

        dao.guardarDesdeServidor(
            respuesta.aCandidata(cvUri = candidata.cvUri)
        )
        return respuesta.id
    }

    suspend fun actualizarEnServidor(candidata: Candidata) {
        require(candidata.id > 0) {
            "La candidata debe tener un ID válido"
        }

        val respuesta = api.actualizarPostulante(
            candidata.id,
            candidata.aPostulanteRequest()
        )

        val cvUriLocal = candidata.cvUri
            ?: dao.obtenerPorId(respuesta.id)?.cvUri

        dao.guardarDesdeServidor(
            respuesta.aCandidata(cvUri = cvUriLocal)
        )
    }

    suspend fun eliminarEnServidor(id: Long) {
        require(id > 0) {
            "La candidata debe tener un ID válido"
        }

        api.eliminarPostulante(id)
        dao.eliminar(id)
    }

    suspend fun cambiarEstadoEnServidor(
        id: Long,
        estado: EstadoCandidatura
    ) {
        require(id > 0) {
            "La candidata debe tener un ID válido"
        }

        val respuesta = api.cambiarEstado(
            id,
            EstadoRequest(estado = estado.name)
        )

        val cvUriLocal = dao.obtenerPorId(id)?.cvUri

        dao.guardarDesdeServidor(
            respuesta.aCandidata(cvUri = cvUriLocal)
        )
    }
}
