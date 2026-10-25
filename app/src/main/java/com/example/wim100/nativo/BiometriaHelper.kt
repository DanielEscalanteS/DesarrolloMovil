package com.example.wim100.nativo

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometriaHelper {

    private fun autenticadores(): Int {
        if (Build.VERSION.SDK_INT >= 30) return BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        return BIOMETRIC_WEAK
    }

    fun problema(context: Context): String? {
        return when (BiometricManager.from(context).canAuthenticate(autenticadores())) {
            BiometricManager.BIOMETRIC_SUCCESS -> null
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Configura una huella o bloqueo en los ajustes de seguridad"
            else -> "La verificación no está disponible en este dispositivo"
        }
    }

    fun autenticar(context: Context, alExito: () -> Unit, alError: (String) -> Unit) {
        val activity = context as? FragmentActivity
        if (activity == null) {
            alError("El contexto no es válido para biometría")
            return
        }

        val error = problema(context)
        if (error != null) {
            alError(error)
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

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Acceso Restringido")
            .setSubtitle(if (Build.VERSION.SDK_INT >= 30) "Usa tu huella o bloqueo para acceder a Dirección" else "Usa tu biometría para acceder a Dirección")
            .setAllowedAuthenticators(autenticadores())
        if (Build.VERSION.SDK_INT < 30) builder.setNegativeButtonText("Cancelar")
        val promptInfo = builder.build()

        biometricPrompt.authenticate(promptInfo)
    }
}