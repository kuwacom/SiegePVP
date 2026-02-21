package dev.kuwa.siegePVP.core.border

import org.bukkit.World
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import org.joml.Math.lerp

class BorderManager(
    private val plugin: Plugin
) {

    private var task: BukkitTask? = null

    /**
     * ワールドボーダーを移動 & 縮小するタスクを開始
     *
     * @param world 移動対象ワールド
     * @param startX 初期中心X
     * @param startZ 初期中心Z
     * @param targetX 終了中心X
     * @param targetZ 終了中心Z
     * @param startSize 初期ボーダーサイズ
     * @param targetSize 終了ボーダーサイズ
     * @param durationTicks タスク長 (ticks)
     */
    fun start(
        world: World,
        startX: Double,
        startZ: Double,
        targetX: Double,
        targetZ: Double,
        startSize: Double,
        targetSize: Double,
        durationTicks: Long
    ) {
        // 既に実行中なら停止してから開始
        stop()

        val border = world.worldBorder

        // 初期値セット
        border.setCenter(startX, startZ)
        border.size = startSize

        var ticks = 0L

        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable{
            ticks++

            val progress = (ticks.toDouble() / durationTicks.toDouble()).coerceAtMost(1.0)

            // 中心位置補間
            val newX = lerp(startX, targetX, progress)
            val newZ = lerp(startZ, targetZ, progress)
            border.setCenter(newX, newZ)

            // サイズ補間
            val newSize = lerp(startSize, targetSize, progress)
            border.size = newSize

            // 完了したら停止
            if (progress >= 1.0) {
                stop()
            }
        }, 0L, 1L)
    }

    /**
     * 現在のタスクを停止（存在する場合）
     */
    fun stop() {
        task?.cancel()
        task = null
    }

    /**
     * 実行中かどうか
     */
    fun isRunning(): Boolean {
        return task != null
    }
}