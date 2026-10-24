package com.example.wim100.nativo

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CvImportado(val uri: String, val nombre: String, val tamano: Long)

/** Copia el documento seleccionado al almacenamiento privado para conservarlo al reiniciar. */
object ArchivoPostulacion {
    suspend fun importar(context: Context, uri: Uri): CvImportado = withContext(Dispatchers.IO) {
        val limite = 5L * 1024 * 1024
        val nombre = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "curriculum.pdf"
        require(nombre.endsWith(".pdf", ignoreCase = true)) { "Selecciona un archivo PDF" }
        val carpeta = File(context.filesDir, "cvs").apply { mkdirs() }
        val destino = File(carpeta, "${UUID.randomUUID()}.pdf")
        try {
            val entrada = context.contentResolver.openInputStream(uri) ?: error("No se pudo abrir el documento")
            var total = 0L
            entrada.use { input ->
                destino.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val cantidad = input.read(buffer)
                        if (cantidad < 0) break
                        total += cantidad
                        require(total <= limite) { "El PDF supera los 5 MB" }
                        output.write(buffer, 0, cantidad)
                    }
                }
            }
            require(total > 0) { "El documento está vacío" }
            ParcelFileDescriptor.open(destino, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { pdf -> require(pdf.pageCount > 0) { "PDF sin páginas" } }
            }
            CvImportado(Uri.fromFile(destino).toString(), nombre, total)
        } catch (e: Exception) {
            destino.delete()
            throw e
        }
    }
}
