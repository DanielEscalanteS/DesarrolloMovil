package com.example.wim100.data.repository

import com.example.wim100.data.remote.RandomUserApi
import com.example.wim100.data.remote.RetrofitClient

class RandomUserRepository(
    private val api: RandomUserApi = RetrofitClient.randomUserApi
) {
    suspend fun obtenerNombreAleatorio(): String {
        val respuesta = api.obtenerNombreAleatorio()

        val nombre = respuesta.results.firstOrNull()?.name
            ?: throw IllegalStateException(
                "No se recibió ningún nombre de RandomUser"
            )

        return "${nombre.first} ${nombre.last}".trim()
    }
}
