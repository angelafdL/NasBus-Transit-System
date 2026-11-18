package com.nasbus.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FareCalculatorTest {

    @Test
    fun testRegularFareCalculation() {
        // 43.8 km * 2.45 farePerKm = 107.31 -> rounded to 107
        val fare1 = FareCalculator.regularFare(43.8, 2.45)
        assertEquals(107, fare1)

        val fare2 = FareCalculator.regularFare(100.0, 2.50)
        assertEquals(250, fare2)

        val fare3 = FareCalculator.regularFare(12.5, 3.84)
        assertEquals(48, fare3)
    }

    @Test
    fun testDiscountedFareCalculation() {
        assertEquals(86, FareCalculator.discountedFare(107))
        assertEquals(134, FareCalculator.discountedFare(167))
        assertEquals(143, FareCalculator.discountedFare(179))
        assertEquals(206, FareCalculator.discountedFare(257))
    }

    @Test
    fun testDiscountRateAndLabel() {
        assertEquals(0.20, FareCalculator.DISCOUNT_RATE, 0.001)
        assertEquals("Student / Senior / PWD", FareCalculator.DISCOUNT_LABEL)
    }

    @Test
    fun testFormatPeso() {
        assertEquals("≈ ₱107", FareCalculator.formatPeso(107))
        assertEquals("≈ ₱206", FareCalculator.formatPeso(206))
    }
}
