package com.example.wim100

import android.app.Application

class WimApplication : Application() {
    // lazy hace que no se encienda la base de datos hasta que se necesite
    val container: com.example.wim100.di.AppContainer by lazy {
        com.example.wim100.di.AppContainer(this)
    }
}