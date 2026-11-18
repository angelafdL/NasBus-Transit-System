package com.nasbus.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHashTest {

    @Test
    fun testSamePasswordProducesDifferentSaltedHashes() {
        val password = "MySecurePassword123!"
        val hash1 = PasswordUtils.hashPassword(password)
        val hash2 = PasswordUtils.hashPassword(password)

        // Random per-user 16-byte salt means hashes must be different
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testPasswordVerificationSuccess() {
        val password = "CorrectPassword123"
        val hash = PasswordUtils.hashPassword(password)

        assertTrue(PasswordUtils.verifyPassword(password, hash))
    }

    @Test
    fun testPasswordVerificationFailure() {
        val password = "CorrectPassword123"
        val hash = PasswordUtils.hashPassword(password)

        assertFalse(PasswordUtils.verifyPassword("WrongPassword123", hash))
    }
}
