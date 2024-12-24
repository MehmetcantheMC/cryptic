package com.memoji.cryptic.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import java.io.RandomAccessFile
import kotlin.math.roundToInt

class SystemMonitor(private val context: Context) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val memoryInfo = ActivityManager.MemoryInfo()
    
    private var prevIdleTime = 0L
    private var prevTotalTime = 0L
    private var prevAppCpuTime = 0L
    private var prevAppTimestamp = System.currentTimeMillis()

    private data class CpuInfo(
        val idle: Long,
        val total: Long
    )

    private fun readCpuInfo(): CpuInfo {
        val reader = RandomAccessFile("/proc/stat", "r")
        val line = reader.readLine()
        reader.close()
        
        val values = line.split(" ").filter { it.isNotEmpty() }
        val user = values[1].toLong()
        val nice = values[2].toLong()
        val system = values[3].toLong()
        val idle = values[4].toLong()
        val iowait = values[5].toLong()
        val irq = values[6].toLong()
        val softirq = values[7].toLong()
        
        val totalIdle = idle + iowait
        val total = user + nice + system + totalIdle + irq + softirq
        
        return CpuInfo(totalIdle, total)
    }

    fun getCpuUsage(): Float {
        try {
            val pid = Process.myPid()
            val path = "/proc/$pid/stat"
            val reader = RandomAccessFile(path, "r")
            val stats = reader.readLine().split(" ")
            reader.close()

            val utime = stats[13].toLong()
            val stime = stats[14].toLong()
            val cutime = stats[15].toLong()
            val cstime = stats[16].toLong()
            
            val appCpuTime = utime + stime + cutime + cstime
            val now = System.currentTimeMillis()
            val timeDiff = now - prevAppTimestamp
            val cpuDiff = appCpuTime - prevAppCpuTime
            
            val usage = if (timeDiff > 0) {
                (100f * cpuDiff) / timeDiff
            } else 0f

            prevAppCpuTime = appCpuTime
            prevAppTimestamp = now

            return usage.coerceIn(0f, 100f)
        } catch (e: Exception) {
            e.printStackTrace()
            return 0f
        }
    }

    fun getTotalCpuUsage(): Float {
        try {
            val cpuInfo = readCpuInfo()
            val diffIdle = cpuInfo.idle - prevIdleTime
            val diffTotal = cpuInfo.total - prevTotalTime
            val usage = if (diffTotal == 0L) 0f else (1f - diffIdle.toFloat() / diffTotal.toFloat()) * 100f
            
            prevIdleTime = cpuInfo.idle
            prevTotalTime = cpuInfo.total
            
            return usage.coerceIn(0f, 100f)
        } catch (e: Exception) {
            e.printStackTrace()
            return 0f
        }
    }

    fun getRamUsage(): Float {
        activityManager.getMemoryInfo(memoryInfo)
        
        // Get app's memory info
        val mi = Runtime.getRuntime()
        val usedMemory = mi.totalMemory() - mi.freeMemory()
        val maxMemory = mi.maxMemory()
        
        // Return the app's memory usage as a percentage of its max allowed memory
        return (usedMemory.toFloat() / maxMemory.toFloat()).coerceIn(0f, 1f)
    }

    fun getTotalRamInfo(): String {
        activityManager.getMemoryInfo(memoryInfo)
        val totalMB = memoryInfo.totalMem / (1024 * 1024)
        val availableMB = memoryInfo.availMem / (1024 * 1024)
        val usedMB = totalMB - availableMB
        return "$usedMB MB / $totalMB MB"
    }
} 