package com.example.wim100.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.example.wim100.data.local.WimDatabase
import com.example.wim100.data.local.CandidataConEvaluaciones
import com.example.wim100.validation.EvaluacionValidator
import com.example.wim100.model.Acciones
import com.example.wim100.model.CalculadoraPuntaje
import com.example.wim100.model.Criterio
import com.example.wim100.model.DetallePuntaje
import com.example.wim100.model.EstadoCandidatura
import com.example.wim100.model.Evaluacion
import com.example.wim100.model.PuntajeCriterio
import com.example.wim100.model.ResultadoConsolidado
import kotlinx.coroutines.flow.Flow

class EvaluacionDuplicadaException(mensaje: String) : Exception(mensaje)

class EvaluacionRepository(
    private val db: WimDatabase,
    private val historial: HistorialRepository
) {
    private val evaluacionDao = db.evaluacionDao()
    private val criterioDao = db.criterioDao()
    private val candidataDao = db.candidataDao()

    fun observarCriterios(): Flow<List<Criterio>> = criterioDao.observarTodos()

    suspend fun obtenerCriterios(): List<Criterio> = criterioDao.obtenerTodos()

    suspend fun asegurarCriterios(criterios: List<Criterio>) {
        if (criterioDao.contar() == 0) {
            criterioDao.insertarTodos(criterios)
        }
    }

    fun observarPorCandidata(candidataId: Long): Flow<List<Evaluacion>> = evaluacionDao.observarPorCandidata(candidataId)
    fun observarDetallePuntajes(candidataId: Long): Flow<List<DetallePuntaje>> = evaluacionDao.observarDetallePuntajes(candidataId)
    fun observarConsolidado(): Flow<List<ResultadoConsolidado>> = evaluacionDao.observarConsolidado()

    fun observarTodas(): Flow<List<Evaluacion>> = evaluacionDao.observarTodas()
    fun observarFicha(candidataId: Long): Flow<CandidataConEvaluaciones?> = evaluacionDao.observarFicha(candidataId)

    suspend fun yaEvaluo(candidataId: Long, evaluadora: String): Boolean {
        val clave = Evaluacion.normalizarClave(evaluadora)
        return evaluacionDao.contarPorEvaluadora(candidataId, clave) > 0
    }

    suspend fun registrar(
        candidataId: Long,
        evaluadora: String,
        puntajes: Map<Long, Int>,
        observaciones: Map<Long, String>,
        observacionGeneral: String
    ): Double {
        val clave = Evaluacion.normalizarClave(evaluadora)

        try {
            // withTransaction (si algo de aquí falla, no se guarda nada a medias)
            return db.withTransaction {

                val candidata = candidataDao.obtenerPorId(candidataId)
                    ?: error("La candidatura no existe")
                check(!candidata.estado.estaResuelta) { "La candidatura ya fue resuelta" }
                require(evaluadora.trim().length >= 3) { "Identifica a la evaluadora" }
                val validacionGeneral = EvaluacionValidator.validarObservacionGeneral(observacionGeneral)
                require(validacionGeneral.esValido) { validacionGeneral.mensaje.orEmpty() }

                // 1. Verifica que no sea duplicada
                if (evaluacionDao.contarPorEvaluadora(candidataId, clave) > 0) {
                    throw EvaluacionDuplicadaException("Ya evaluaste a esta candidata")
                }

                // 2. Calcula la nota
                val criterios = criterioDao.obtenerTodos()
                check(criterios.isNotEmpty()) { "Espera a que se carguen los criterios" }
                for (criterio in criterios) {
                    val nota = EvaluacionValidator.validarPuntaje(puntajes[criterio.id])
                    require(nota.esValido) { "${criterio.nombre}: ${nota.mensaje}" }
                    val observacion = EvaluacionValidator.validarObservacionCriterio(
                        puntajes[criterio.id], observaciones[criterio.id].orEmpty())
                    require(observacion.esValido) { "${criterio.nombre}: ${observacion.mensaje}" }
                }
                val total = CalculadoraPuntaje.calcular(criterios, puntajes)

                // 3. Guarda la evaluación
                val evaluacionId = evaluacionDao.insertar(
                    Evaluacion(
                        candidataId = candidataId,
                        evaluadora = evaluadora,
                        evaluadoraClave = clave,
                        puntajeTotal = total,
                        observacionGeneral = observacionGeneral
                    )
                )

                // 4. Guarda los puntajes 1 a 5 de los criterios
                val listaPuntajes = criterios.map { c ->
                    PuntajeCriterio(
                        evaluacionId = evaluacionId,
                        criterioId = c.id,
                        puntaje = puntajes[c.id] ?: 0,
                        observacion = observaciones[c.id] ?: ""
                    )
                }
                evaluacionDao.insertarPuntajes(listaPuntajes)

                // 5. Lo actualiza si la candidata ya tiene 2 notas
                val cantidadEvaluaciones = evaluacionDao.contarPorCandidata(candidataId)

                val nuevoEstado = if (cantidadEvaluaciones >= Evaluacion.MINIMO_PARA_CONSOLIDAR) {
                    EstadoCandidatura.EVALUADA
                } else {
                    EstadoCandidatura.EN_EVALUACION
                }
                candidataDao.actualizarEstado(candidataId, nuevoEstado)

                // 6. Registro al historial
                historial.registrar(candidataId, Acciones.EVALUACION, "Evaluación registrada", evaluadora)

                total // Se retorna la nota final
            }
        } catch (e: EvaluacionDuplicadaException) {
            historial.registrar(candidataId, Acciones.INTENTO_DUPLICADO, "El sistema bloqueó un duplicado", evaluadora)
            throw e
        } catch (e: SQLiteConstraintException) {
            historial.registrar(candidataId, Acciones.INTENTO_DUPLICADO, "El sistema bloqueó un duplicado", evaluadora)
            throw EvaluacionDuplicadaException("Error interno: Ya evaluaste")
        }
    }
}
