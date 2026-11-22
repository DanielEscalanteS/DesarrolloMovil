package com.example.wim100.data.remote

import com.example.wim100.model.EstadoCandidatura
import com.example.wim100.viewmodel.candidataRemotaTest
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ErroresYMapperTest {

    private fun http(codigo: Int, json: String = ""): HttpException =
        HttpException(
            Response.error<Any>(codigo, json.toResponseBody("application/json".toMediaType()))
        )

    @Test
    fun mensajesParaUsuarioCubrenCodigosHttpYRed() {
        assertEquals(
            "Correo duplicado",
            http(400, """{"status":400,"mensaje":"Error","errores":{"correo":"Correo duplicado"}}""").mensajeParaUsuario()
        )
        assertEquals(
            "Mensaje personalizado",
            http(400, """{"status":400,"mensaje":"Mensaje personalizado"}""").mensajeParaUsuario()
        )
        assertEquals("Los datos ingresados no son válidos", http(400).mensajeParaUsuario())
        assertEquals("Usuario o contraseña incorrectos", http(401).mensajeParaUsuario())
        assertEquals("El recurso solicitado no existe", http(404).mensajeParaUsuario())
        assertEquals("Ya existe una candidata con ese correo", http(409).mensajeParaUsuario())
        assertEquals("El servidor presenta un problema", http(500).mensajeParaUsuario())
        assertEquals("Error del servidor: 418", http(418).mensajeParaUsuario())
        assertEquals(
            "No se pudo conectar con el servidor. Revisa tu conexión",
            IOException().mensajeParaUsuario()
        )
        assertEquals("Fallo puntual", IllegalStateException("Fallo puntual").mensajeParaUsuario())
    }

    @Test
    fun mappersConviertenEntreDtoYModelo() {
        val candidata = candidataRemotaTest(10L)
        val request = candidata.aPostulanteRequest()
        assertEquals(candidata.correo, request.correo)

        val dto = PostulanteDto(
            id = 10L,
            nombre = candidata.nombre,
            correo = candidata.correo,
            telefono = candidata.telefono,
            area = candidata.area,
            aniosExperiencia = candidata.aniosExperiencia,
            resumenExperiencia = candidata.resumenExperiencia,
            linkedinUrl = candidata.linkedinUrl,
            cvNombre = candidata.cvNombre,
            estado = "EVALUADA",
            fechaRegistro = 123L,
            registradaPor = candidata.registradaPor
        )
        val reconvertida = dto.aCandidata("file:///local.pdf")
        assertEquals(EstadoCandidatura.EVALUADA, reconvertida.estado)
        assertEquals("file:///local.pdf", reconvertida.cvUri)
    }
}