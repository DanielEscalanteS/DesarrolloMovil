package com.example.wim100.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface WimApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @GET("postulantes")
    suspend fun listarPostulantes(): List<PostulanteDto>

    @GET("postulantes/{id}")
    suspend fun obtenerPostulante(
        @Path("id") id: Long
    ): PostulanteDto

    @POST("postulantes")
    suspend fun crearPostulante(
        @Body request: PostulanteRequest
    ): PostulanteDto

    @PUT("postulantes/{id}")
    suspend fun actualizarPostulante(
        @Path("id") id: Long,
        @Body request: PostulanteRequest
    ): PostulanteDto

    @PATCH("postulantes/{id}/estado")
    suspend fun cambiarEstado(
        @Path("id") id: Long,
        @Body request: EstadoRequest
    ): PostulanteDto

    @DELETE("postulantes/{id}")
    suspend fun eliminarPostulante(
        @Path("id") id: Long
    )
}
