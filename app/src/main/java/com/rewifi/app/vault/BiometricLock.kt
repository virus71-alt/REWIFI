package com.rewifi.app.vault

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Wraps the system biometric prompt. Falls back to device PIN/pattern/password
 * via DEVICE_CREDENTIAL, so users without a fingerprint sensor still get a lock.
 */
object BiometricLock {

    fun isAvailable(activity: FragmentActivity): Boolean {
        val auth = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return BiometricManager.from(activity)
            .canAuthenticate(auth) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun prompt(activity: FragmentActivity, onSuccess: () -> Unit, onFail: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                override fun onAuthenticationError(code: Int, msg: CharSequence) = onFail()
            })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("REWIFI")
            .setSubtitle("Unlock your WiFi vault")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(info)
    }
}
