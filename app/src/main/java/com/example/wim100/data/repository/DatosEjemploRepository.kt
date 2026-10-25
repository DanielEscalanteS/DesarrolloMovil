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
    companion object {
        const val RESPONSABLE = "Ejemplos WIM100"
        const val PREPARACION = "Ejemplos iniciales v2"
    }

    // La app prepara los ejemplos una sola vez. Los avances posteriores se conservan.
    suspend fun cargar() = withContext(Dispatchers.IO) {
        val archivosNuevos = mutableListOf<File>()
        try {
            db.withTransaction {
                if (db.historialDao().contarAccion(PREPARACION, RESPONSABLE) > 0) return@withTransaction
                evaluaciones.asegurarCriterios(CriteriosDemostracion.lista)
                val criterios = evaluaciones.obtenerCriterios()
                check(criterios.map { it.orden } == listOf(1, 2, 3, 4, 5)) { "Los ejemplos necesitan los cinco criterios de la matriz." }
                data class Ejemplo(val candidata: Candidata, val notas: List<Int>?, val evaluadora: String = "Ana Pérez",
                    val estado: EstadoCandidatura = EstadoCandidatura.EVALUADA)
                val ejemplos = listOf(
                    Ejemplo(ejemplo("Camila Rojas", "camila.rojas.demo@example.com", AREAS_ESPECIALIDAD[1], 3), null),
                    Ejemplo(ejemplo("Francisca Muñoz", "francisca.munoz.demo@example.com", AREAS_ESPECIALIDAD[1], 4), listOf(5, 4, 2, 3, 4)),
                    Ejemplo(ejemplo("Paula Herrera", "paula.herrera.demo@example.com", AREAS_ESPECIALIDAD[1], 5), listOf(3, 4, 5, 5, 4)),
                    Ejemplo(ejemplo("Daniela Vega", "daniela.vega.demo@example.com", AREAS_ESPECIALIDAD[1], 4), listOf(5, 5, 3, 3, 2), "Beatriz López"),
                    Ejemplo(ejemplo("Catalina Torres", "catalina.torres.demo@example.com", AREAS_ESPECIALIDAD[1], 6), listOf(4, 4, 4, 4, 4), estado = EstadoCandidatura.SELECCIONADA),
                    Ejemplo(ejemplo("Valentina Soto", "valentina.soto.demo@example.com", AREAS_ESPECIALIDAD[0], 5), listOf(4, 5, 4, 4, 5)),
                    Ejemplo(ejemplo("Antonia Silva", "antonia.silva.demo@example.com", AREAS_ESPECIALIDAD[0], 3), listOf(5, 5, 2, 4, 3), "Beatriz López"),
                    Ejemplo(ejemplo("Fernanda Díaz", "fernanda.diaz.demo@example.com", AREAS_ESPECIALIDAD[0], 1), listOf(2, 2, 3, 2, 3), "Beatriz López", EstadoCandidatura.NO_SELECCIONADA)
                )
                for (ej in ejemplos) {
                    val candidata = ej.candidata
                    if (candidatas.existeCorreo(candidata.correo)) {
                        // Solo se reorganiza el área de nuestros ejemplos anteriores, no sus notas ni resoluciones.
                        db.candidataDao().actualizarAreaEjemplo(candidata.correo, RESPONSABLE, candidata.area)
                        continue
                    }
                    val archivo = crearCv(candidata, archivosNuevos)
                    val id = candidatas.registrar(candidata.copy(cvUri = Uri.fromFile(archivo).toString(),
                        cvNombre = "CV_${candidata.nombre.replace(' ', '_')}.pdf"))
                    if (ej.notas != null) {
                        val puntajes = criterios.mapIndexed { i, c -> c.id to ej.notas[i] }.toMap()
                        val observaciones = criterios.filter { checkNotNull(puntajes[it.id]) <= 2 }.associate {
                            it.id to "El perfil ficticio necesita aportar más evidencia en ${it.nombre.lowercase()}."
                        }
                        evaluaciones.registrar(id, ej.evaluadora, puntajes, observaciones,
                            "Perfil ficticio de demostración. Las notas reflejan fortalezas y aspectos por mejorar según la matriz.")
                        if (ej.estado.estaResuelta) candidatas.resolver(id, ej.estado, Sesion("Dirección Demo", Rol.DIRECCION))
                    }
                }
                historial.registrar(null, PREPARACION, "Se prepararon ocho perfiles ficticios para consultar, evaluar y comparar por área.", RESPONSABLE)
            }
        } catch (e: Exception) {
            archivosNuevos.forEach { it.delete() }
            throw e
        }
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

}
