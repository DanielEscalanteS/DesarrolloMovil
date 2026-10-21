package com.example.appsemestral

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.wim100.ui.WimApp
import com.example.wim100.ui.theme.AppSemestralTheme

/** FragmentActivity (y no ComponentActivity) porque BiometricPrompt la necesita. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppSemestralTheme {
                WimApp()
            }
        }
    }
}