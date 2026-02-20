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
    private val gameManager: dev.kuwa.siegePVP.core.game.GameManager,
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

        // FastBoard がまだなければ作成
        val board = boards.getOrPut(uuid) {
            FastBoard(player).apply {
                updateTitle("§e§lプレイ情報")
            }
        }

        // 非同期で死亡数取得
        playerManager.getPlayerDeathCount(uuid)?.thenAcceptAsync({ deathCount ->

            if (player.scoreboardTags.contains("admin")) {
                // 管理者用サイドバー
                updateAdminBoard(board)
            } else {
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
            }
        }, { task -> Bukkit.getScheduler().runTask(plugin, task) }) // メインスレッドで実行
    }

    /**
     * 管理者用サイドバーの更新
     */
    private fun updateAdminBoard(board: FastBoard) {
        // ゲーム状態の取得
        gameManager.getState()?.thenAccept { state ->
            val stateName = when (state) {
                dev.kuwa.siegePVP.core.game.GameState.WAITING -> "§7§l待機中"
                dev.kuwa.siegePVP.core.game.GameState.STARTING -> "§e§l開始準備中"
                dev.kuwa.siegePVP.core.game.GameState.RUNNING -> "§a§l進行中"
                dev.kuwa.siegePVP.core.game.GameState.ENDED -> "§c§l終了"
                else -> "§f§l不明"
            }

            // 全オンラインプレイヤー
            val allPlayers = MultiLib.getAllOnlinePlayers()
            val totalPlayers = allPlayers.size

            // 赤チーム・青チームの生存者数
            val redAlive = teamManager.countAliveSurvivalTeam("red", allPlayers)
            val blueAlive = teamManager.countAliveSurvivalTeam("blue", allPlayers)

            // ボス情報の取得
            val bosses = allPlayers.filter { it.scoreboardTags.contains("boss") }
            val bossInfo = if (bosses.isEmpty()) {
                "§c§lなし"
            } else {
                bosses.joinToString("\n") { boss ->
                    val bossTeam = boss.scoreboard.getEntryTeam(boss.name)?.name
                    val teamColor = when (bossTeam) {
                        "red" -> "§c赤"
                        "blue" -> "§9青"
                        else -> "§f?"
                    }
                    val status = if (boss.gameMode == org.bukkit.GameMode.SURVIVAL) "§a生存" else "§c死亡"
                    "$teamColor: §f${boss.name} ($status)"
                }
            }

            // タイマー設定時間
            val timerDuration = timer.duration

            board.updateLines(
                "§6§l管理画面",
                "",
                "§b§lゲーム状態: $stateName",
                "§b§l残り時間: §a§l" + formatSeconds(timer.getRemainingSeconds()),
                "§b§l設定時間: §f" + formatSeconds(timerDuration),
                "",
                "§b§lオンライン: §f$totalPlayers 人",
                "§b§l赤チーム生存: §c§l$redAlive",
                "§b§l青チーム生存: §9§l$blueAlive",
                "",
                "§b§l死亡上限: §f${playerManager.gameOverDeathCount}",
                "",
                "§b§lボス情報:",
                bossInfo
            )
        }
    }
}
