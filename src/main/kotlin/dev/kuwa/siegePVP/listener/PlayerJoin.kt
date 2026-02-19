package dev.kuwa.siegePVP.listener

import dev.kuwa.siegePVP.core.game.GameManager
import dev.kuwa.siegePVP.core.game.GameState
import dev.kuwa.siegePVP.core.player.PlayerManager
import dev.kuwa.siegePVP.core.team.TeamManager
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin

class PlayerJoin(
    private val plugin: Plugin,
    private val gameManager: GameManager,
    private val teamManager: TeamManager,
    private val playerManager: PlayerManager
) : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // ゲーム中のみ実行
        gameManager.getState()?.thenAcceptAsync( { state ->
            // adminの場合、ゲームに参加しないようにする
            if (state == GameState.RUNNING && !player.scoreboardTags.contains("admin")) {
                if (player.scoreboardTags.contains("boss")) {
                    // ボスプレイヤー用処理
                } else {
                    // 一般プレイヤー用処理
                    teamManager.allocateTeam(player)
                }
            }
        }, { task -> Bukkit.getScheduler().runTask(plugin, task) }) // メインスレッドで実行
    }

}