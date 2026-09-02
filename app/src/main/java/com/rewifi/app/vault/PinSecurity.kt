package com.rewifi.app.vault

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Cryptographic utility for REWIFI app-specific PIN.
 *
 * Uses PBKDF2WithHmacSHA256 with 100,000 iterations and a 32-byte secure random salt.
 * Verifies hashes in constant time to prevent side-channel timing attacks.
 * Plain-text PINs are never persisted or logged.
 */
object PinSecurity {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_SIZE_BYTES = 32

    /** Generate 32 bytes of cryptographically secure random salt. */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_SIZE_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /** Derive a 256-bit cryptographic hash from the PIN and salt using PBKDF2. */
    fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    /** Verify an entered PIN against the stored hash and salt in constant time. */
    fun verifyPin(enteredPin: String, salt: ByteArray, expectedHash: ByteArray): Boolean {
        val computed = hashPin(enteredPin, salt)
        return MessageDigest.isEqual(computed, expectedHash)
    }

    fun toBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    fun fromBase64(str: String): ByteArray = Base64.decode(str, Base64.NO_WRAP)
}
