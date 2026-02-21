package dev.kuwa.siegePVP.listener

import dev.kuwa.siegePVP.core.game.GameManager
import dev.kuwa.siegePVP.core.game.GameState
import dev.kuwa.siegePVP.core.player.PlayerManager
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class PlayerDeathListener(
    private val plugin: Plugin,
    private val gameManager: GameManager,
    private val playerManager: PlayerManager
) : Listener {
    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val player = event.entity
//        player.sendMessage("あなたは死んだ！")
        plugin.logger.info(player.scoreboardTags.toString())
        // ゲーム中のみ実行
        gameManager.getState()?.thenAcceptAsync( { state ->
            // adminの場合、ゲームに参加しないようにする
            if (state == GameState.RUNNING &&
                !player.scoreboardTags.contains("admin")) {

                player.addPotionEffect(
                    PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 3, 255, false, false)
                )

                if (player.scoreboardTags.contains("boss")) {
                    // ボスプレイヤー用処理
                    gameManager.onBossDeath(player)
                } else {
                    // 一般プレイヤー用処理
                    playerManager.onPlayerDeath(player)
                }
            }
        }, { task -> Bukkit.getScheduler().runTask(plugin, task) }) // メインスレッドで実行
    }
}
