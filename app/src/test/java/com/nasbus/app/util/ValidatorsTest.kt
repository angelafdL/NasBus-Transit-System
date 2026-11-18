package com.nasbus.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun testUsernameValidation() {
        assertTrue(Validators.isValidUsername("juan_delacruz"))
        assertTrue(Validators.isValidUsername("  user123  ")) // Trimmed space
        assertTrue(Validators.isValidUsername("abc"))

        assertFalse(Validators.isValidUsername("ab")) // Too short (< 3)
        assertFalse(Validators.isValidUsername("user@domain")) // Invalid character '@'
        assertFalse(Validators.isValidUsername("a_very_long_username_exceeding_20_chars")) // Too long (> 20)
    }

    @Test
    fun testEmailValidation() {
        assertTrue(Validators.isValidEmail("user@example.com"))
        assertTrue(Validators.isValidEmail("  user.name+tag@domain.co.ph  ")) // Trimmed space

        assertFalse(Validators.isValidEmail("invalid-email"))
        assertFalse(Validators.isValidEmail("user@"))
        assertFalse(Validators.isValidEmail("@domain.com"))
    }

    @Test
    fun testPasswordValidation() {
        assertTrue(Validators.isValidPassword("password123"))
        assertTrue(Validators.isValidPassword("12345678"))

        assertFalse(Validators.isValidPassword("1234567")) // Less than 8 chars
        assertFalse(Validators.isValidPassword(""))
    }

    @Test
    fun testPasswordsMatch() {
        assertTrue(Validators.doPasswordsMatch("password123", "password123"))
        assertFalse(Validators.doPasswordsMatch("password123", "password321"))
    }
}
