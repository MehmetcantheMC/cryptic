package com.memoji.cryptic.utils

import java.security.MessageDigest
import kotlin.experimental.and
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class PasswordCracker {
    companion object {
        private val CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf(
            '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '_', '+', '=',
            '[', ']', '{', '}', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '.',
            '?', '/', '`', '~', ' '
        )

        const val PACKET_SIZE = 60000L
        const val MAX_PASSWORD_LENGTH = 8 // Maximum password length to try
        const val BATCH_SIZE = 100 // Process passwords in larger batches
        
        // Get number of available processors at runtime
        private val PARALLEL_THREADS = Runtime.getRuntime().availableProcessors()

        fun getNextJobPacket(packetNumber: Int): JobPacket {
            var remainingPackets = packetNumber
            var currentLength = 1
            var totalProcessedPositions = 0L
            
            while (currentLength <= MAX_PASSWORD_LENGTH) {
                val combinationsForLength = getTotalCombinations(currentLength)
                val packetsNeededForLength = (combinationsForLength + PACKET_SIZE - 1) / PACKET_SIZE
                
                if (remainingPackets < packetsNeededForLength) {
                    // This is the length we're working on
                    val startPosition = remainingPackets * PACKET_SIZE
                    val endPosition = minOf(startPosition + PACKET_SIZE - 1, combinationsForLength - 1)
                    
                    println("""
                        Debug Info:
                        Length: $currentLength
                        Combinations for this length: $combinationsForLength
                        Packets needed for this length: $packetsNeededForLength
                        Current packet within this length: $remainingPackets
                        Range: $startPosition - $endPosition
                    """.trimIndent())
                    
                    return JobPacket(startPosition, endPosition, packetNumber, currentLength)
                }
                
                totalProcessedPositions += combinationsForLength
                remainingPackets -= packetsNeededForLength.toInt()
                currentLength++
            }
            
            // If we get here, we've tried all lengths up to MAX_PASSWORD_LENGTH
            return JobPacket(-1, -1, packetNumber, -1)
        }

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

        suspend fun attemptCrackParallel(
            targetHash: String,
            startPosition: Long,
            endPosition: Long,
            length: Int,
            coroutineScope: CoroutineScope
        ): CrackResult? {
            val batchSize = (endPosition - startPosition + PARALLEL_THREADS - 1) / PARALLEL_THREADS
            
            val deferreds = (0 until PARALLEL_THREADS).map { threadIndex ->
                coroutineScope.async(Dispatchers.Default) {
                    val threadStart = startPosition + threadIndex * batchSize
                    val threadEnd = minOf(threadStart + batchSize, endPosition)
                    
                    if (threadStart >= endPosition) return@async null
                    
                    (threadStart until threadEnd).forEach { position ->
                        val password = generatePasswordAtPosition(position, length)
                        val hash = generateMD5(password)
                        if (hash == targetHash) {
                            return@async CrackResult(password, hash, position, true)
                        }
                    }
                    null
                }
            }
            
            return deferreds.awaitAll().filterNotNull().firstOrNull() ?: CrackResult(
                generatePasswordAtPosition(endPosition - 1, length),
                "",
                endPosition - 1,
                false
            )
        }

        // Calculate total possible combinations for a given length
        fun getTotalCombinations(length: Int): Long {
            var total: Long = 1
            val charSetSize = CHARS.size.toLong()
            repeat(length) {
                total = if (total > Long.MAX_VALUE / charSetSize) {
                    Long.MAX_VALUE
                } else {
                    total * charSetSize
                }
            }
            return total
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

    data class JobPacket(
        val startPosition: Long,
        val endPosition: Long,
        val packetNumber: Int,
        val passwordLength: Int
    ) {
        val isValid: Boolean
            get() = passwordLength > 0
    }
} 