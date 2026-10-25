
package com.example.wim100

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.wim100.ui.WimApp
import com.example.wim100.ui.theme.WIM100Theme

// FragmentActivity para poder utilizar el BiometricPrompt
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            WIM100Theme {
                WimApp()
            }
        }
    }
}
