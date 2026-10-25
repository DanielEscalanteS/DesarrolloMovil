package com.example.wim100.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.wim100.model.RegistroHistorial
import com.example.wim100.model.Candidata
import com.example.wim100.model.EstadoCandidatura
import kotlinx.coroutines.flow.Flow

@Dao
interface CandidataDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(candidata: Candidata): Long

    @Insert
    suspend fun insertarHistorial(registro: RegistroHistorial): Long

    @Transaction
    suspend fun insertarConHistorial(candidata: Candidata, registro: RegistroHistorial): Long {
        val id = insertar(candidata)
        insertarHistorial(registro.copy(candidataId = id))
        return id
    }

    // Flow (para que la lista se actualice sola en la pantalla si alguien agrega una candidata)
    @Query("SELECT * FROM candidatas ORDER BY fechaRegistro DESC")
    fun observarTodas(): Flow<List<Candidata>>

    @Query("SELECT * FROM candidatas WHERE id = :id")
    fun observarPorId(id: Long): Flow<Candidata?>

    @Query("SELECT * FROM candidatas WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Candidata?

    @Query("UPDATE candidatas SET estado = :estado WHERE id = :id")
    suspend fun actualizarEstado(id: Long, estado: EstadoCandidatura)

    @Query("SELECT COUNT(*) FROM candidatas")
    suspend fun contar(): Int

    @Query("SELECT COUNT(*) FROM candidatas WHERE LOWER(correo) = LOWER(:correo)")
    suspend fun contarPorCorreo(correo: String): Int

    @Query("DELETE FROM candidatas WHERE id = :id")
    suspend fun eliminar(id: Long): Int

    @Query("SELECT COUNT(*) FROM candidatas WHERE cvUri = :uri")
    suspend fun contarPorCvUri(uri: String): Int

    @Query("""
        UPDATE candidatas SET estado = :nuevoEstado
        WHERE id = :id AND estado = :estadoActual
        AND (SELECT COUNT(*) FROM evaluaciones WHERE candidataId = :id) >= :minimo
    """)
    suspend fun resolverSiEvaluada(id: Long, nuevoEstado: EstadoCandidatura,
        estadoActual: EstadoCandidatura, minimo: Int): Int

    @Transaction
    suspend fun resolverConHistorial(id: Long, nuevoEstado: EstadoCandidatura,
        registro: RegistroHistorial, minimo: Int) {
        val actualizadas = resolverSiEvaluada(id, nuevoEstado, EstadoCandidatura.EVALUADA, minimo)
        check(actualizadas == 1) { "La candidatura debe estar evaluada y no resuelta" }
        insertarHistorial(registro)
    }
}
