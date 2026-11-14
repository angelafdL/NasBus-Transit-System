package com.nasbus.app.util

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Utility for secure password hashing and verification.
 *
 * WHY PLAIN-TEXT PASSWORDS ARE BAD:
 * Storing plain-text passwords in a database is a critical security vulnerability.
 * If the local database is breached, exposed, or extracted, plain-text passwords
 * immediately compromise user accounts across all services where they reuse the same password.
 * Hashing with a per-user random salt using SHA-256 ensures that even if database records
 * are leaked, original passwords cannot be reversed or looked up via rainbow tables.
 */
object PasswordUtils {

    private const val SALT_BYTE_LENGTH = 16

    fun hashPassword(password: String): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(SALT_BYTE_LENGTH)
        random.nextBytes(saltBytes)

        val saltHex = bytesToHex(saltBytes)
        val hashHex = computeSha256(saltHex + password)

        return "$saltHex:$hashHex"
    }

    fun verifyPassword(password: String, storedPasswordHash: String): Boolean {
        val parts = storedPasswordHash.split(":")
        if (parts.size != 2) return false

        val saltHex = parts[0]
        val expectedHashHex = parts[1]

        val computedHashHex = computeSha256(saltHex + password)

        // Constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
            computedHashHex.toByteArray(Charsets.UTF_8),
            expectedHashHex.toByteArray(Charsets.UTF_8)
        )
    }

    private fun computeSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytesToHex(hashBytes)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
