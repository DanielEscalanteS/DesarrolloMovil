
package com.example.wim100

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.wim100.ui.WimApp
import com.example.wim100.ui.theme.WIM100Theme

class MainActivity : FragmentActivity() {

    private val solicitarPermiso = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            mostrarAplicacion()
        } else {
            mostrarSolicitudPermiso()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (
            Build.VERSION.SDK_INT >= 37 &&
            checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            mostrarSolicitudPermiso()
            solicitarPermiso.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        } else {
            mostrarAplicacion()
        }
    }

    private fun mostrarAplicacion() {
        setContent {
            WIM100Theme {
                WimApp()
            }
        }
    }

    private fun mostrarSolicitudPermiso() {
        setContent {
            WIM100Theme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "WIM 100 necesita acceso a la red local para conectarse al servidor."
                    )

                    Button(
                        onClick = {
                            solicitarPermiso.launch(
                                Manifest.permission.ACCESS_LOCAL_NETWORK
                            )
                        }
                    ) {
                        Text("Permitir acceso a la red local")
                    }
                }
            }
        }
    }
}
