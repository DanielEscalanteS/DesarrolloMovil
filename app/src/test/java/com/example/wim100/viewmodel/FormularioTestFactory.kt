package com.example.wim100.viewmodel

import com.example.wim100.data.local.dao.CandidataDao
import com.example.wim100.data.local.dao.HistorialDao
import com.example.wim100.data.repository.CandidataRepository
import com.example.wim100.data.repository.HistorialRepository
import com.example.wim100.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Los tests de validación no deben abrir una base Android. */
internal fun crearFormularioViewModel(): FormularioViewModel {
    val candidatas = object : CandidataDao {
        override suspend fun insertar(candidata: Candidata): Long = error("No se debe guardar en un test de validación")
        override suspend fun insertarHistorial(registro: RegistroHistorial): Long = error("No se debe guardar")
        override fun observarTodas(): Flow<List<Candidata>> = flowOf(emptyList())
        override fun observarPorId(id: Long): Flow<Candidata?> = flowOf(null)
        override suspend fun obtenerPorId(id: Long): Candidata? = null
        override suspend fun actualizarEstado(id: Long, estado: EstadoCandidatura) = Unit
        override suspend fun resolverSiEvaluada(id: Long, nuevoEstado: EstadoCandidatura,
            estadoActual: EstadoCandidatura, minimo: Int): Int = 0
        override suspend fun contar(): Int = 0
        override suspend fun contarPorCorreo(correo: String): Int = 0
        override suspend fun eliminar(id: Long): Int = error("No se debe borrar en un test de validación")
        override suspend fun contarPorCvUri(uri: String): Int = 0
    }
    val historial = object : HistorialDao {
        override suspend fun insertar(registro: RegistroHistorial): Long = error("No se debe guardar")
        override fun observarTodo(): Flow<List<RegistroHistorial>> = flowOf(emptyList())
        override fun observarPorCandidata(candidataId: Long): Flow<List<RegistroHistorial>> = flowOf(emptyList())
        override suspend fun eliminarPorCandidata(candidataId: Long): Unit = error("No se debe borrar")
    }
    return FormularioViewModel(CandidataRepository(candidatas, HistorialRepository(historial)))
}
