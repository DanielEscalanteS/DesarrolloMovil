package com.example.wim100

import android.app.Application
import com.example.wim100.di.AppContainer

class WimApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
