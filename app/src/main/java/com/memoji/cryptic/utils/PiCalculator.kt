package com.memoji.cryptic.utils

import kotlin.math.pow

class PiCalculator {
    companion object {
        private var piString = "3.14159265358979323846264338327950288419716939937510"

        fun calculatePiDigit(n: Int): Int {
            if (n < 0) return 0
            if (n == 0) return 3

            // For digits after decimal point
            if (n <= piString.length - 2) {
                return piString[n + 1].toString().toInt()
            }

            // If we need more digits, calculate them using Nilakantha series
            var pi = 3.0
            var operation = 1
            var denominator = 2
            
            // More iterations for better accuracy
            for (i in 0..1000) {
                pi += operation * (4.0 / (denominator * (denominator + 1) * (denominator + 2)))
                operation *= -1
                denominator += 2
            }

            // Convert to string and get the requested digit
            val digits = (pi * 10.0.pow(n + 1)).toInt()
            return digits % 10
        }

        // Get a range of digits as a string
        fun getPiDigits(start: Int, count: Int): String {
            val result = StringBuilder()
            for (i in start until start + count) {
                result.append(calculatePiDigit(i))
            }
            return result.toString()
        }
    }
} 