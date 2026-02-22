package dev.kuwa.siegePVP.listener

import dev.kuwa.siegePVP.core.game.GameManager
import dev.kuwa.siegePVP.core.game.GameState
import dev.kuwa.siegePVP.core.player.PlayerManager
import dev.kuwa.siegePVP.core.team.TeamManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
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

        // ゲーム中もしくはスタート中のみ実行
        gameManager.getState()?.thenAcceptAsync( { state ->
            // adminの場合、ゲームに参加しないようにする
            if ((state == GameState.RUNNING || state == GameState.STARTING) &&
                !player.scoreboardTags.contains("admin")) {
                if (player.scoreboardTags.contains("boss")) {
                    // ボスプレイヤー用処理
                } else {
                    // 一般プレイヤー用処理
                    // まだチームに参加してなかったら
                    if (!teamManager.hasTeam(player)) {
                        // チームに割り当て
                        // スポーンポイントを変更してチームスポーンポイントへtp
                        val team = teamManager.allocateTeam(player)
                        team?.let {
                            teamManager.getTeamSpawnLocation(it.name).thenAcceptAsync( { location ->
                                player.bedSpawnLocation = location
                                location?.let { player.teleport(it) }
                            }, { task -> Bukkit.getScheduler().runTask(plugin, task) })
                        }

                        if (state == GameState.RUNNING) {
                            player.gameMode = GameMode.SURVIVAL
                            // プレイヤーの状態を全てリセット
                            playerManager.resetPlayerState(player)
                        } else {
                            // スタート中に入った場合はまだゲーム中ではないのでアドベンチャー
                            player.gameMode = GameMode.ADVENTURE
                        }
                    }
                }
            } else {
//                // ゲーム中じゃなければスポーンポイント削除
//                player.bedSpawnLocation = null
            }
        }, { task -> Bukkit.getScheduler().runTask(plugin, task) }) // メインスレッドで実行
    }

}