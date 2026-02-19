package dev.kuwa.siegePVP.core.task

import com.github.puregero.multilib.MultiLib
import dev.kuwa.siegePVP.core.player.PlayerManager
import dev.kuwa.siegePVP.core.team.TeamManager
import dev.kuwa.siegePVP.utils.formatSeconds
import fr.mrmicky.fastboard.FastBoard
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

class PlayerScoreboardUpdater(
    private val plugin: Plugin,
    private val teamManager: TeamManager,
    private val playerManager: PlayerManager,
    private val timer: Timer
) {

    private val boards = mutableMapOf<UUID, FastBoard>()
    private var task: BukkitTask? = null

    fun start() {
        stop()

        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            updateAll()
        }, 0L, 20L)
    }

    fun stop() {
        task?.cancel()
        task = null

        boards.values.forEach { it.delete() }
        boards.clear()
    }

    private fun updateAll() {
//        plugin.server.onlinePlayers.forEach { updateFor(it) }
        val onlineUuids = plugin.server.onlinePlayers.map { it.uniqueId }.toSet()

        // offlineのプレイヤーのplayerインスタンスを削除
        boards.keys.filter { it !in onlineUuids }.forEach { uuid ->
            boards.remove(uuid)?.delete()
        }

        plugin.server.onlinePlayers.forEach { updateFor(it) }
    }

    private fun updateFor(player: Player) {
        val uuid = player.uniqueId

        if (player.scoreboardTags.contains("admin")) {
            return
        }

        // FastBoard がまだなければ作成
        val board = boards.getOrPut(uuid) {
            FastBoard(player).apply {
                updateTitle("§e§lプレイ情報")
            }
        }

        // 非同期で死亡数取得
        playerManager.getPlayerDeathCount(uuid)?.thenAcceptAsync({ deathCount ->

            val remaining = (playerManager.gameOverDeathCount - deathCount).coerceAtLeast(0)

            val team = player.scoreboard.getEntryTeam(player.name)
            val teamName = when (team?.name) {
                "red"  -> "§c§l赤"
                "blue" -> "§9§l青"
                else   -> "なし"
            }
            val color = team?.color ?: ChatColor.RESET
            val teamDisplay = "$color$teamName"

            val survivalCount = teamManager.countAliveSurvivalTeam(team?.name, MultiLib.getAllOnlinePlayers())

            if (player.scoreboardTags.contains("boss")) {
                // ボスプレイヤーサイドバー
                board.updateLines(
                    "",
                    "§b§l残り時間: §a§l" + formatSeconds(timer.getRemainingSeconds()),
                    "§b§lチーム: §l$teamName",
                    "§b§l生存者: §a§l$survivalCount"
                )
            } else {
                // 通常プレイヤーサイドバー
                board.updateLines(
                    "",
                    "§b§l残り時間: §a§l" + formatSeconds(timer.getRemainingSeconds()),
                    "§b§l残基: §a§l$remaining",
                    "§b§lチーム: §l$teamName",
                    "§b§l生存者: §a§l$survivalCount"
                )
            }
        }, { task -> Bukkit.getScheduler().runTask(plugin, task) }) // メインスレッドで実行
    }
}