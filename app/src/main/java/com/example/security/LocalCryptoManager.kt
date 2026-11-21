package com.example.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object LocalCryptoManager {
    private const val PBKDF2_ALGO = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 12_000
    private const val KEY_LENGTH = 256

    fun generateSaltBase64(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hashSecret(secret: String, saltBase64: String): String {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGO)
        val encoded = factory.generateSecret(spec).encoded
        return Base64.encodeToString(encoded, Base64.NO_WRAP)
    }

    fun verifySecret(secret: String, saltBase64: String, expectedHash: String): Boolean {
        val actual = hashSecret(secret, saltBase64)
        return MessageDigest.isEqual(actual.toByteArray(), expectedHash.toByteArray())
    }

    fun generateRecoveryCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rng = SecureRandom()
        val raw = (1..12).map { alphabet[rng.nextInt(alphabet.length)] }.joinToString("")
        return "${raw.substring(0, 4)}-${raw.substring(4, 8)}-${raw.substring(8, 12)}"
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
