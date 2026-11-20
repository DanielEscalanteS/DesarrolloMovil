package com.example.wim100.data.local

import androidx.room.withTransaction
import com.example.wim100.model.CriteriosDemostracion

object DatosSemilla {

    suspend fun preparar(database: WimDatabase) {
        database.withTransaction {
            val criterioDao = database.criterioDao()

            if (criterioDao.contar() == 0) {
                criterioDao.insertarTodos(
                    CriteriosDemostracion.lista
                )
            }
        }
    }
}
