package com.example.wim100.data.repository

import com.example.wim100.data.remote.LoginRequest
import com.example.wim100.data.remote.RetrofitClient
import com.example.wim100.data.remote.WimApi
import com.example.wim100.model.Rol
import com.example.wim100.model.Sesion

class AuthRepository(
    private val api: WimApi = RetrofitClient.wimApi
) {
    suspend fun iniciarSesion(
        usuario: String,
        password: String
    ): Sesion {
        val respuesta = api.login(
            LoginRequest(
                usuario = usuario.trim(),
                password = password
            )
        )

        return Sesion(
            nombre = respuesta.nombre,
            rol = Rol.valueOf(respuesta.rol),
            id = respuesta.id,
            usuario = respuesta.usuario
        )
    }
}
