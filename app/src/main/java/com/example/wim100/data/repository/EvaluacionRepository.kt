package com.example.wim100.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.example.wim100.data.local.CandidataConEvaluaciones
import com.example.wim100.data.local.WimDatabase
import com.example.wim100.data.remote.EvaluacionDto
import com.example.wim100.data.remote.PuntajeDto
import com.example.wim100.data.remote.RetrofitClient
import com.example.wim100.data.remote.WimApi
import com.example.wim100.model.Acciones
import com.example.wim100.model.CalculadoraPuntaje
import com.example.wim100.model.Criterio
import com.example.wim100.model.DetallePuntaje
import com.example.wim100.model.EstadoCandidatura
import com.example.wim100.model.Evaluacion
import com.example.wim100.model.PuntajeCriterio
import com.example.wim100.model.ResultadoConsolidado
import com.example.wim100.model.Rol
import com.example.wim100.model.Sesion
import com.example.wim100.validation.EvaluacionValidator
import kotlinx.coroutines.flow.Flow

class EvaluacionDuplicadaException(mensaje: String) : Exception(mensaje)

class EvaluacionRepository(
    private val db: WimDatabase,
    private val historial: HistorialRepository,
    private val api: WimApi = RetrofitClient.wimApi
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
    suspend fun obtenerFicha(candidataId: Long): CandidataConEvaluaciones? = evaluacionDao.obtenerFicha(candidataId)
    fun observarFichas(): Flow<List<CandidataConEvaluaciones>> = evaluacionDao.observarFichas()

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
            return db.withTransaction {
                val candidata = candidataDao.obtenerPorId(candidataId)
                    ?: error("La candidatura no existe")
                check(!candidata.estado.estaResuelta) { "La candidatura ya fue resuelta" }
                require(evaluadora.trim().length >= 3) { "Identifica a la evaluadora" }
                val validacionGeneral = EvaluacionValidator.validarObservacionGeneral(observacionGeneral)
                require(validacionGeneral.esValido) { validacionGeneral.mensaje.orEmpty() }

                if (evaluacionDao.contarPorCandidata(candidataId) > 0) {
                    throw EvaluacionDuplicadaException("Esta candidata ya tiene una evaluación. Su responsable puede editarla.")
                }

                val criterios = criterioDao.obtenerTodos()
                check(criterios.isNotEmpty()) { "Espera a que se carguen los criterios" }
                for (criterio in criterios) {
                    val nota = EvaluacionValidator.validarPuntaje(puntajes[criterio.id])
                    require(nota.esValido) { "${criterio.nombre}: ${nota.mensaje}" }
                    val observacion = EvaluacionValidator.validarObservacionCriterio(
                        puntajes[criterio.id], observaciones[criterio.id].orEmpty()
                    )
                    require(observacion.esValido) { "${criterio.nombre}: ${observacion.mensaje}" }
                }
                val total = CalculadoraPuntaje.calcular(criterios, puntajes)

                val evaluacionId = evaluacionDao.insertar(
                    Evaluacion(
                        candidataId = candidataId,
                        evaluadora = evaluadora,
                        evaluadoraClave = clave,
                        puntajeTotal = total,
                        observacionGeneral = observacionGeneral
                    )
                )

                val listaPuntajes = criterios.map { c ->
                    PuntajeCriterio(
                        evaluacionId = evaluacionId,
                        criterioId = c.id,
                        puntaje = puntajes[c.id] ?: 0,
                        observacion = observaciones[c.id] ?: ""
                    )
                }
                evaluacionDao.insertarPuntajes(listaPuntajes)

                val cantidadEvaluaciones = evaluacionDao.contarPorCandidata(candidataId)
                val nuevoEstado = if (cantidadEvaluaciones >= Evaluacion.MINIMO_PARA_CONSOLIDAR) {
                    EstadoCandidatura.EVALUADA
                } else {
                    EstadoCandidatura.EN_EVALUACION
                }
                candidataDao.actualizarEstado(candidataId, nuevoEstado)

                historial.registrar(candidataId, Acciones.EVALUACION, "Evaluación registrada", evaluadora)

                total
            }
        } catch (e: EvaluacionDuplicadaException) {
            historial.registrar(candidataId, Acciones.INTENTO_DUPLICADO, "El sistema bloqueó un duplicado", evaluadora)
            throw e
        } catch (e: SQLiteConstraintException) {
            if (evaluacionDao.contarPorCandidata(candidataId) == 0) throw e
            historial.registrar(candidataId, Acciones.INTENTO_DUPLICADO, "El sistema bloqueó un duplicado", evaluadora)
            throw EvaluacionDuplicadaException("Esta candidata ya tiene una evaluación")
        }
    }

    suspend fun actualizar(
        candidataId: Long,
        sesion: Sesion,
        revisionEsperada: Int,
        puntajes: Map<Long, Int>,
        observaciones: Map<Long, String>,
        observacionGeneral: String
    ): Double = db.withTransaction {
        require(sesion.rol == Rol.EVALUADORA) { "Solo la evaluadora responsable puede editar" }
        val candidata = candidataDao.obtenerPorId(candidataId) ?: error("La candidatura no existe")
        check(!candidata.estado.estaResuelta) { "Dirección ya resolvió esta candidatura" }
        val anterior = evaluacionDao.obtenerPorCandidata(candidataId) ?: error("La evaluación no existe")
        require(anterior.evaluadoraClave == Evaluacion.normalizarClave(sesion.nombre)) {
            "Solo ${anterior.evaluadora} puede modificar esta evaluación"
        }
        check(anterior.revision == revisionEsperada) { "La evaluación cambió. Vuelve a abrirla antes de editar." }
        val general = EvaluacionValidator.validarObservacionGeneral(observacionGeneral)
        require(general.esValido) { general.mensaje.orEmpty() }
        val criterios = criterioDao.obtenerTodos()
        check(criterios.isNotEmpty()) { "No se cargaron los criterios" }
        for (criterio in criterios) {
            val nota = EvaluacionValidator.validarPuntaje(puntajes[criterio.id])
            require(nota.esValido) { "${criterio.nombre}: ${nota.mensaje}" }
            val observacion = EvaluacionValidator.validarObservacionCriterio(puntajes[criterio.id], observaciones[criterio.id].orEmpty())
            require(observacion.esValido) { "${criterio.nombre}: ${observacion.mensaje}" }
        }
        val anteriores = evaluacionDao.obtenerPuntajes(anterior.id).associateBy { it.criterioId }
        val total = CalculadoraPuntaje.calcular(criterios, puntajes)
        check(evaluacionDao.actualizar(anterior.id, total, observacionGeneral, System.currentTimeMillis(), revisionEsperada) == 1) {
            "La evaluación cambió. Vuelve a abrirla antes de editar."
        }
        evaluacionDao.eliminarPuntajes(anterior.id)
        evaluacionDao.insertarPuntajes(criterios.map { c ->
            PuntajeCriterio(
                evaluacionId = anterior.id,
                criterioId = c.id,
                puntaje = checkNotNull(puntajes[c.id]),
                observacion = observaciones[c.id].orEmpty()
            )
        })
        val cambios = criterios.joinToString(" | ") { c ->
            "${c.nombre}: ${anteriores[c.id]?.puntaje} → ${puntajes[c.id]}. " +
                    "Justificación anterior: ${anteriores[c.id]?.observacion.orEmpty()}. Nueva: ${observaciones[c.id].orEmpty()}"
        }
        historial.registrar(
            candidataId,
            Acciones.EDICION_EVALUACION,
            "Total: ${anterior.puntajeTotal} → $total. $cambios. Observación anterior: ${anterior.observacionGeneral}. Nueva: $observacionGeneral",
            sesion.nombre
        )
        total
    }

    suspend fun enviarAlServidor(
        candidataId: Long,
        evaluadora: String,
        puntajes: Map<Long, Int>,
        observaciones: Map<Long, String>,
        observacionGeneral: String
    ) {
        val criterios = criterioDao.obtenerTodos()
        val total = CalculadoraPuntaje.calcular(criterios, puntajes)
        val dto = EvaluacionDto(
            postulanteId = candidataId,
            evaluadora = evaluadora.trim(),
            evaluadoraClave = Evaluacion.normalizarClave(evaluadora),
            puntajeTotal = total,
            observacionGeneral = observacionGeneral.trim(),
            fecha = System.currentTimeMillis(),
            puntajes = criterios.map { c ->
                PuntajeDto(
                    criterioId = c.id,
                    puntaje = puntajes[c.id] ?: 0,
                    justificacion = observaciones[c.id].orEmpty().trim()
                )
            }
        )
        api.guardarEvaluacion(dto)
    }

    suspend fun sincronizarDesdeServidor() {
        val remotas = api.listarEvaluaciones()
        db.withTransaction {
            val criterios = criterioDao.obtenerTodos()
            if (criterios.isEmpty()) return@withTransaction

            for (dto in remotas) {
                val candidata = candidataDao.obtenerPorId(dto.postulanteId) ?: continue
                val existente = evaluacionDao.obtenerPorCandidata(dto.postulanteId)

                val evaluacionId = if (existente == null) {
                    evaluacionDao.insertar(
                        Evaluacion(
                            candidataId = dto.postulanteId,
                            evaluadora = dto.evaluadora,
                            evaluadoraClave = dto.evaluadoraClave,
                            puntajeTotal = dto.puntajeTotal,
                            observacionGeneral = dto.observacionGeneral,
                            fecha = dto.fecha
                        )
                    )
                } else {
                    evaluacionDao.actualizar(
                        id = existente.id,
                        total = dto.puntajeTotal,
                        observacion = dto.observacionGeneral,
                        fecha = dto.fecha,
                        revision = existente.revision
                    )
                    evaluacionDao.eliminarPuntajes(existente.id)
                    existente.id
                }

                val listaPuntajes = dto.puntajes.map { p ->
                    PuntajeCriterio(
                        evaluacionId = evaluacionId,
                        criterioId = p.criterioId,
                        puntaje = p.puntaje,
                        observacion = p.justificacion
                    )
                }
                if (listaPuntajes.isNotEmpty()) {
                    evaluacionDao.insertarPuntajes(listaPuntajes)
                }

                if (!candidata.estado.estaResuelta && candidata.estado != EstadoCandidatura.EVALUADA) {
                    candidataDao.actualizarEstado(dto.postulanteId, EstadoCandidatura.EVALUADA)
                }
            }
        }
    }
}