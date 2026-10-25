package com.example.wim100.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.wim100.model.RegistroHistorial
import kotlinx.coroutines.flow.Flow

@Dao
interface HistorialDao {
    @Insert
    suspend fun insertar(registro: RegistroHistorial): Long

    @Query("SELECT * FROM historial ORDER BY fecha DESC, id DESC")
    fun observarTodo(): Flow<List<RegistroHistorial>>

    @Query("SELECT * FROM historial WHERE candidataId = :candidataId ORDER BY fecha DESC, id DESC")
    fun observarPorCandidata(candidataId: Long): Flow<List<RegistroHistorial>>

    @Query("DELETE FROM historial WHERE candidataId = :candidataId")
    suspend fun eliminarPorCandidata(candidataId: Long)
}
