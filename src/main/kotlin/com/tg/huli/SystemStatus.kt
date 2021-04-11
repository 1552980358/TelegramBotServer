package com.tg.huli

import lib.github1552980358.ktExtension.jvm.keyword.tryOnly
import lib.github1552980358.ktExtension.jvm.lang.execCmd
import com.tg.huli.Utils.Companion.getAsMiB
import kotlinx.coroutines.delay
import java.io.File

class SystemStatus private constructor() {

    companion object {
        val instance by lazy { SystemStatus() }

        private const val PATH_PROC_STAT = "/proc/stat"
    }

    private val stringBuilder = StringBuilder()
    private val tmpStringBuilder = StringBuilder()
    private var lines = arrayListOf<String>()
    private var usage = 0.0

    private fun getPhysicalMemory(string: String): String {
        string.split(" ").toMutableList().apply {
            removeAll { it.isEmpty() || it == " " }
            return "- Physical RAM: ${this[2].toInt() / this[1].toInt() * 100}% , ${getAsMiB(this[2])} MiB / ${getAsMiB(this[1])} MiB"
        }
    }

    private fun getSwapMemory(string: String): String {
        string.split(" ").toMutableList().apply {
            removeAll { it.isEmpty() || it == " " }
            return "- Swap: ${this[2].toInt() / this[1].toInt() * 100}% , ${getAsMiB(this[2])} MiB / ${getAsMiB(this[1])} MiB"
        }
    }

    private fun getMemoryStatus(): String? {
        lines.clear()
        execCmd("free").apply { waitFor() }
            .inputStream
            .bufferedReader()
            .use { bufferedReader ->
                tryOnly { lines = bufferedReader.readLines() as ArrayList<String> }
            }
        if (lines.isEmpty()) {
            return null
        }

        // Remove first line
        tmpStringBuilder.clear()
        tmpStringBuilder.append(getPhysicalMemory(lines[1]))
        if (lines.size == 3) {
            tmpStringBuilder.append('\n')
            tmpStringBuilder.append(getSwapMemory(lines[2]))
        }
        return tmpStringBuilder.append('\n').toString()
    }

    private fun getDiff(str1: String, str2: String): Double {
        return str1.toDouble() - str2.toDouble()
    }

    private suspend fun getCPUUsage(): String? {
        tmpStringBuilder.clear()
        try {
            File(PATH_PROC_STAT).bufferedReader().use { tmpStringBuilder.append(readLine()) }
        } catch (e: Exception) {
            return null
        }
        lines.clear()
        tmpStringBuilder.split(" ").toMutableList()
            .apply {
                removeAll { it.isEmpty() || it == " " }
                try {
                    for (i in 0 until 4) {
                        lines.add(this[i + 1])
                    }
                } catch (e: Exception) {
                    return null
                }
            }
        delay(1000)
        try {
            File(PATH_PROC_STAT).bufferedReader().use { tmpStringBuilder.append(readLine()) }
        } catch (e: Exception) {
            return null
        }
        try {
            tmpStringBuilder.split(" ").toMutableList()
                .apply {
                    removeAll { it.isEmpty() || it == " " }
                    usage = (getDiff(this[1], lines[0]) + // user
                            getDiff(this[2], lines[1]) + // user low
                            getDiff(this[3], lines[2])  // sys
                            )
                    return (usage * 100 / (usage + getDiff(this[4], lines[3]))).toInt().toString() + '\n'
                }
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun getAllStatus(): String? {
        stringBuilder.clear()
        getCPUUsage().apply {
            if (this.isNullOrEmpty()) {
                return null
            }
            stringBuilder.append(this)
        }
        getMemoryStatus().apply {
            if (this.isNullOrEmpty()) {
                return null
            }
            stringBuilder.append(this)
        }
        return stringBuilder.toString()
    }

    suspend fun getUpdate(): String {
        return getAllStatus()?: return "Error when updating server status!"
    }

}

fun getSystemStatus() = SystemStatus.instance