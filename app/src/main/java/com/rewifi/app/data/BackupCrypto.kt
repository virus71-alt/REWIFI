package com.rewifi.app.data

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Passphrase-based encryption for *portable* backups.
 *
 * Unlike [Crypto] (hardware-bound, dies on factory reset), this derives the key
 * from a user passphrase via PBKDF2, so a backup file can be decrypted on a brand
 * new device after a reset/crash — which is the whole point of the app.
 *
 * Wire format:  [ salt(16) | iv(12) | ciphertext+tag ]
 */
object BackupCrypto {
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    const val MAGIC = "REWIFI1"   // prefix so we can sanity-check files

    fun encrypt(plain: ByteArray, passphrase: CharArray): ByteArray {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
        val iv = cipher.iv
        val ct = cipher.doFinal(plain)
        return MAGIC.toByteArray(Charsets.UTF_8) + salt + iv + ct
    }

    fun decrypt(blob: ByteArray, passphrase: CharArray): ByteArray {
        val magic = MAGIC.toByteArray(Charsets.UTF_8)
        require(blob.size > magic.size + SALT_LEN + IV_LEN) { "File too small / not a REWIFI backup" }
        require(blob.copyOfRange(0, magic.size).contentEquals(magic)) { "Not a REWIFI backup file" }
        var p = magic.size
        val salt = blob.copyOfRange(p, p + SALT_LEN); p += SALT_LEN
        val iv = blob.copyOfRange(p, p + IV_LEN); p += IV_LEN
        val ct = blob.copyOfRange(p, blob.size)
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        }
        return cipher.doFinal(ct)   // throws AEADBadTagException if passphrase is wrong
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }
}
