package com.example.wim100.nativo

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometriaHelper {

    fun autenticar(context: Context, alExito: () -> Unit, alError: (String) -> Unit) {
        val activity = context as? FragmentActivity
        if (activity == null) {
            alError("El contexto no es válido para biometría")
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                // Si la huella es correcta, ejecutamos la acción de éxito
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    alExito()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    alError(errString.toString())
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Acceso Restringido")
            .setSubtitle("Usa tu huella o PIN para acceder a Dirección")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}