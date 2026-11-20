package com.example.wim100.data.remote

import retrofit2.http.GET

data class RandomUserResponse(
    val results: List<RandomUserResultado>
)

data class RandomUserResultado(
    val name: RandomUserNombre
)

data class RandomUserNombre(
    val first: String,
    val last: String
)

interface RandomUserApi {

    @GET("api/?gender=female&nat=es&inc=name")
    suspend fun obtenerNombreAleatorio(): RandomUserResponse
}
