package com.example.wim100.data.repository

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.room.withTransaction
import com.example.wim100.data.local.WimDatabase
import com.example.wim100.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class DatosEjemploRepository(
    private val context: Context,
    private val db: WimDatabase,
    private val candidatas: CandidataRepository,
    private val evaluaciones: EvaluacionRepository,
    private val historial: HistorialRepository
) {
    companion object { const val RESPONSABLE = "Ejemplos WIM100" }

    // Se agregan al pedirlo desde Inicio. No se vacía la base al abrir la app.
    suspend fun cargar(reemplazarId: Long?): String = withContext(Dispatchers.IO) {
        val archivosNuevos = mutableListOf<File>()
        var anterior: Candidata? = null
        var creadas = 0
        try {
            db.withTransaction {
                if (reemplazarId != null) {
                    anterior = db.candidataDao().obtenerPorId(reemplazarId)
                        ?: error("El registro anterior ya no está disponible.")
                    check(anterior?.registradaPor != RESPONSABLE) { "Selecciona el registro de prueba anterior." }
                    db.historialDao().eliminarPorCandidata(reemplazarId)
                    check(db.candidataDao().eliminar(reemplazarId) == 1)
                }
                evaluaciones.asegurarCriterios(CriteriosDemostracion.lista)
                val criterios = evaluaciones.obtenerCriterios()
                check(criterios.map { it.orden } == listOf(1, 2, 3, 4, 5)) { "Los ejemplos necesitan los cinco criterios de la matriz." }

                val ejemplos = listOf(
                    ejemplo("Camila Rojas", "camila.rojas.demo@example.com", AREAS_ESPECIALIDAD[1], 3),
                    ejemplo("Valentina Soto", "valentina.soto.demo@example.com", AREAS_ESPECIALIDAD[0], 5),
                    ejemplo("Francisca Muñoz", "francisca.munoz.demo@example.com", AREAS_ESPECIALIDAD[2], 4)
                )
                ejemplos.forEachIndexed { indice, candidata ->
                    // Repetir la carga conserva las evaluaciones y resoluciones de los ejemplos existentes.
                    if (!candidatas.existeCorreo(candidata.correo)) {
                        val archivo = crearCv(candidata, archivosNuevos)
                        val id = candidatas.registrar(candidata.copy(cvUri = Uri.fromFile(archivo).toString(), cvNombre = "CV_${candidata.nombre.replace(' ', '_')}.pdf"))
                        creadas++
                        val notas = when (indice) {
                            1 -> listOf(4, 5, 4, 4, 5)
                            2 -> listOf(5, 4, 4, 5, 4)
                            else -> null
                        }
                        if (notas != null) {
                            evaluaciones.registrar(id, "Ana Pérez", criterios.mapIndexed { i, c -> c.id to notas[i] }.toMap(),
                                emptyMap(), "Ejemplo ficticio: demuestra experiencia y conocimientos relevantes para el área.")
                        }
                        if (indice == 2) {
                            val otrasNotas = listOf(3, 4, 2, 3, 4)
                            evaluaciones.registrar(id, "Beatriz López", criterios.mapIndexed { i, c -> c.id to otrasNotas[i] }.toMap(),
                                mapOf(criterios[2].id to "Falta evidencia de coordinación de equipos en este ejemplo."),
                                "Ejemplo ficticio: debe fortalecer su liderazgo y aportar más evidencia de gestión.")
                        }
                    }
                }
                if (anterior != null) historial.registrar(null, "Reemplazo de datos de prueba",
                    "Se retiró el registro ${anterior?.nombre} y se prepararon los ejemplos.", RESPONSABLE)
            }
        } catch (e: Exception) {
            archivosNuevos.forEach { it.delete() }
            throw e
        }
        val archivoRetirado = retirarCvAnterior(anterior)
        "$creadas candidatas de ejemplo agregadas. Las que ya existían se conservaron." +
                (if (anterior != null) " Se retiró el registro anterior." else "") +
                (if (!archivoRetirado) " No se pudo borrar el archivo anterior; su referencia ya fue retirada." else "")
    }

    private fun ejemplo(nombre: String, correo: String, area: String, anios: Int) = Candidata(
        nombre = nombre, correo = correo, telefono = "912345678", area = area,
        aniosExperiencia = anios,
        resumenExperiencia = "Perfil ficticio con experiencia en análisis de riesgos, respuesta a incidentes y trabajo en equipo.",
        linkedinUrl = "https://www.linkedin.com/in/perfil-ficticio-wim100",
        cvUri = null, cvNombre = null, registradaPor = RESPONSABLE
    )

    private fun crearCv(candidata: Candidata, nuevos: MutableList<File>): File {
        val carpeta = File(context.filesDir, "cvs").apply { mkdirs() }
        val archivo = File(carpeta, "ejemplo_${UUID.randomUUID()}.pdf")
        nuevos.add(archivo)
        val documento = PdfDocument()
        try {
            val pagina = documento.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            try {
                val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f }
                val lineas = listOf(
                    "CV FICTICIO - SOLO PARA DEMOSTRACION", "", candidata.nombre, candidata.correo,
                    "Área: ${candidata.area}", "Experiencia: ${candidata.aniosExperiencia} años", "",
                    "Perfil de ejemplo para probar postulaciones y evaluaciones WIM100.",
                    "Experiencia simulada en análisis de riesgos y respuesta a incidentes.",
                    "Participación ficticia en equipos y actividades de ciberseguridad.", "",
                    "Este documento no corresponde a una persona ni a un CV real."
                )
                lineas.forEachIndexed { indice, texto -> pagina.canvas.drawText(texto, 40f, 60f + indice * 26f, pincel) }
            } finally {
                documento.finishPage(pagina)
            }
            archivo.outputStream().use { documento.writeTo(it) }
        } finally {
            documento.close()
        }
        return archivo
    }

    private suspend fun retirarCvAnterior(candidata: Candidata?): Boolean {
        val uri = candidata?.cvUri ?: return true
        return try {
            if (db.candidataDao().contarPorCvUri(uri) > 0) return true
            val guardada = Uri.parse(uri)
            if (guardada.scheme != "file") return false
            val archivo = File(guardada.path ?: return false).canonicalFile
            val carpeta = File(context.filesDir, "cvs").canonicalFile
            if (archivo.parentFile != carpeta) return false
            !archivo.exists() || archivo.delete()
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { false }
    }
}
