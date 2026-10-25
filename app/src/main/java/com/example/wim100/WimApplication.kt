package com.example.wim100

import android.app.Application

class WimApplication : Application() {
    // lazy hace que no se encienda la base de datos hasta que se necesite
    val container: AppContainer by lazy { AppContainer(this) }
}