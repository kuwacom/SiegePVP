package dev.kuwa.siegePVP.core.task

import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

class Timer(
    private val plugin: Plugin,
    private val onTimeUp: () -> Unit
) {
    private var task: BukkitTask? = null
    private var startMillis = 0L
    var duration = 60L * 30L // 30分（秒）

    fun start() {
        stop()

        startMillis = System.currentTimeMillis()

        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val elapsed = (System.currentTimeMillis() - startMillis) / 1000
            if (elapsed >= duration) {
                stop()
                onTimeUp()
            }
        }, 0L, 1L) // 1 Tick ごと（できるだけ細かく）
    }

    fun stop() {
        task?.cancel()
        task = null
        startMillis = 0L
    }

    fun getRemainingSeconds(): Long {
        val elapsed = (System.currentTimeMillis() - startMillis) / 1000
        return (duration - elapsed).coerceAtLeast(0L)
    }
}
