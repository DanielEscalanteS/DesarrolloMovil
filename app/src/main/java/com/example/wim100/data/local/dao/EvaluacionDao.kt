package com.example.wim100.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.wim100.data.local.CandidataConEvaluaciones
import com.example.wim100.model.DetallePuntaje
import com.example.wim100.model.Evaluacion
import com.example.wim100.model.PuntajeCriterio
import com.example.wim100.model.ResultadoConsolidado
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluacionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(evaluacion: Evaluacion): Long

    @Insert
    suspend fun insertarPuntajes(puntajes: List<PuntajeCriterio>)

    @Query("SELECT COUNT(*) FROM evaluaciones WHERE candidataId = :candidataId AND evaluadoraClave = :clave")
    suspend fun contarPorEvaluadora(candidataId: Long, clave: String): Int

    @Query("SELECT COUNT(*) FROM evaluaciones WHERE candidataId = :candidataId")
    suspend fun contarPorCandidata(candidataId: Long): Int

    @Query("SELECT * FROM evaluaciones WHERE candidataId = :candidataId ORDER BY fecha DESC")
    fun observarPorCandidata(candidataId: Long): Flow<List<Evaluacion>>

    @Query("SELECT * FROM evaluaciones ORDER BY fecha DESC, id DESC")
    fun observarTodas(): Flow<List<Evaluacion>>

    @Transaction
    @Query("SELECT * FROM candidatas WHERE id = :candidataId")
    fun observarFicha(candidataId: Long): Flow<CandidataConEvaluaciones?>

    @Transaction
    @Query("SELECT * FROM candidatas WHERE id = :candidataId")
    suspend fun obtenerFicha(candidataId: Long): CandidataConEvaluaciones?

    @Transaction
    @Query("SELECT * FROM candidatas ORDER BY nombre")
    fun observarFichas(): Flow<List<CandidataConEvaluaciones>>

    @Query("SELECT * FROM evaluaciones WHERE candidataId = :candidataId LIMIT 1")
    suspend fun obtenerPorCandidata(candidataId: Long): Evaluacion?

    @Query("SELECT * FROM puntajes_criterio WHERE evaluacionId = :evaluacionId")
    suspend fun obtenerPuntajes(evaluacionId: Long): List<PuntajeCriterio>

    @Query("DELETE FROM puntajes_criterio WHERE evaluacionId = :evaluacionId")
    suspend fun eliminarPuntajes(evaluacionId: Long)

    @Query("""
        UPDATE evaluaciones SET puntajeTotal = :total, observacionGeneral = :observacion,
            fechaEdicion = :fecha, revision = revision + 1
        WHERE id = :id AND revision = :revision
    """)
    suspend fun actualizar(id: Long, total: Double, observacion: String, fecha: Long, revision: Int): Int

    @Query(
        """
        SELECT p.evaluacionId AS evaluacionId, c.nombre AS criterioNombre, c.peso AS peso,
               p.puntaje AS puntaje, p.observacion AS observacion
        FROM puntajes_criterio p
        INNER JOIN criterios c ON c.id = p.criterioId
        INNER JOIN evaluaciones e ON e.id = p.evaluacionId
        WHERE e.candidataId = :candidataId
        ORDER BY c.orden
        """
    )
    fun observarDetallePuntajes(candidataId: Long): Flow<List<DetallePuntaje>>

    @Query(
        """
        SELECT c.id AS candidataId, c.nombre AS nombre, c.area AS area, c.estado AS estado,
               COUNT(e.id) AS totalEvaluaciones, AVG(e.puntajeTotal) AS promedio
        FROM candidatas c
        LEFT JOIN evaluaciones e ON e.candidataId = c.id
        GROUP BY c.id
        ORDER BY promedio DESC, c.nombre ASC
        """
    )
    fun observarConsolidado(): Flow<List<ResultadoConsolidado>>
}
