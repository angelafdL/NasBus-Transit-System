package com.nasbus.app.util

import kotlin.math.roundToInt

object FareCalculator {
    const val DISCOUNT_RATE = 0.20
    const val DISCOUNT_LABEL = "Student / Senior / PWD"

    fun regularFare(distanceKm: Double, farePerKm: Double): Int {
        return (distanceKm * farePerKm).roundToInt()
    }

    fun discountedFare(regularFare: Int): Int {
        return (regularFare * (1 - DISCOUNT_RATE)).roundToInt()
    }

    fun formatPeso(amount: Int): String {
        return "≈ ₱$amount"
    }
}
