package com.example.data.security

import java.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinSecurity {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val SALT_BYTES = 16

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(pin: String, salt: String): String {
        val saltBytes = Base64.getDecoder().decode(salt)
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            Base64.getEncoder().encodeToString(factory.generateSecret(spec).encoded)
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(pin: String, expectedHash: String, salt: String): Boolean {
        if (pin.length < 4 || expectedHash.isBlank() || salt.isBlank()) return false
        val actual = hash(pin, salt)
        return MessageDigest.isEqual(
            Base64.getDecoder().decode(actual),
            Base64.getDecoder().decode(expectedHash)
        )
    }
}
