package com.memoji.cryptic.utils

import java.security.MessageDigest
import kotlin.experimental.and

class PasswordCracker {
    companion object {
        private val CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf(
            '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '_', '+', '=',
            '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '.',
            '?', '/', '`', '~', ' '
        )

        fun generateMD5(input: String): String {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            return digest.joinToString("") { "%02x".format(it and 0xFF.toByte()) }
        }

        fun generatePasswordAtPosition(position: Long, length: Int): String {
            if (position < 0) return "".padStart(length, CHARS[0])
            
            val base = CHARS.size
            var tempPosition = position
            val chars = CharArray(length)
            
            // Fill from right to left
            for (i in length - 1 downTo 0) {
                val index = (tempPosition % base).toInt()
                chars[i] = CHARS[index]
                tempPosition /= base
            }
            
            // If we still have remaining positions, pad with first character
            for (i in chars.indices) {
                if (chars[i] == '\u0000') {
                    chars[i] = CHARS[0]
                }
            }
            
            return String(chars)
        }

        fun attemptCrack(targetHash: String, currentPosition: Long, length: Int): CrackResult {
            val password = generatePasswordAtPosition(currentPosition, length)
            val hash = generateMD5(password)
            
            return CrackResult(
                password = password,
                hash = hash,
                attempt = currentPosition,
                found = hash == targetHash
            )
        }

        // Calculate total possible combinations for a given length
        fun getTotalCombinations(length: Int): Long {
            var total: Long = 1
            repeat(length) {
                total *= CHARS.size
            }
            return total
        }

        // Calculate the range for a specific node
        fun calculateRange(totalNodes: Int, nodeIndex: Int, passwordLength: Int): SearchRange {
            val totalCombinations = getTotalCombinations(passwordLength)
            val rangeSize = totalCombinations / totalNodes
            val start = rangeSize * nodeIndex
            val end = if (nodeIndex == totalNodes - 1) {
                totalCombinations - 1 // Last node takes remaining combinations
            } else {
                (rangeSize * (nodeIndex + 1)) - 1
            }
            return SearchRange(start, end)
        }

        // Get the character set size
        fun getCharacterSetSize(): Int = CHARS.size
    }

    data class CrackResult(
        val password: String,
        val hash: String,
        val attempt: Long,
        val found: Boolean
    )

    data class SearchRange(
        val start: Long,
        val end: Long
    ) {
        val size: Long get() = end - start + 1
    }
} 