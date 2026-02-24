package dev.kuwa.siegePVP.core.player

import com.github.puregero.multilib.DataStorageImpl
import com.github.puregero.multilib.MultiLib
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture

class PlayerManager(
    private val plugin: Plugin,
    private val dataStorage: DataStorageImpl,
) {
    var gameOverDeathCount = 4;

    fun resetAllPlayerDeathCount() {
        MultiLib.getAllOnlinePlayers().forEach {
            resetPlayerDeathCount(it.uniqueId)
        }
    }

    fun resetPlayerDeathCount(uuid: UUID) {
        dataStorage.set("siege.GameManager.playerDeathCount.$uuid", 0)
    }

    fun addPlayerDeathCount(uuid: UUID) {
        dataStorage.add("siege.GameManager.playerDeathCount.$uuid", 1)
    }

    fun getPlayerDeathCount(uuid: UUID): CompletableFuture<Int>? {
        return dataStorage.getInt("siege.GameManager.playerDeathCount.$uuid", 0)
    }

    fun resetPlayerState(player: Player) {
        //　死亡回数リセット
        resetPlayerDeathCount(player.uniqueId)

        // インベントリ削除
        player.inventory.clear()
        player.inventory.armorContents = emptyArray()

        // ポーション効果削除
        player.activePotionEffects.forEach {
            player.removePotionEffect(it.type)
        }

        // 体力回復(最大体力が多い場合はそれに合わせる)
        player.health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0

        // 満腹度回復
        player.foodLevel = 20
        player.saturation = 20f
        player.exhaustion = 0f

        // 火消し
        player.fireTicks = 0

        // 経験値リセット
        player.totalExperience = 0
        player.level = 0
        player.exp = 0f

        // 落下距離リセット
        player.fallDistance = 0f

        // パンを16個配布
        player.inventory.addItem(ItemStack(Material.BREAD, 16))

        // サーバーに登録されているすべての実績をイテレート
        Bukkit.getServer().advancementIterator().forEachRemaining { advancement ->
            val progress = player.getAdvancementProgress(advancement)

            // プレイヤーが既に獲得している実績の条件（criteria）を取得し、すべて取り消す
            val awardedCriteria = progress.awardedCriteria.toList() // ConcurrentModificationExceptionを防ぐためにリスト化
            for (criterion in awardedCriteria) {
                progress.revokeCriteria(criterion)
            }
        }
    }

    /**
     * プレイヤー死亡時
     */
    fun onPlayerDeath(player: Player) {
        // 一般プレイヤー用処理
        val uuid = player.uniqueId

        getPlayerDeathCount(uuid)?.thenAcceptAsync({ count ->
            // データ取得非同期でカウント判定

            addPlayerDeathCount(uuid) // else カウント入れた方が、スペクテイター後に死亡したときの処理楽かも
            // +1 することで、表示の残基が0でちょうどgameoverになる(計測が0基準だから)
            if (count + 1 >= gameOverDeathCount) {
                onPlayerGameOver(player)
            }
        }, { task -> Bukkit.getScheduler().runTask(plugin, task) }) // メインスレッドで実行
    }

    /**
     * ゲームオーバー時
     */
    private fun onPlayerGameOver(player: Player) {
        val mainTitle = Component.text("§c§lGAME OVER")
        val subTitle = Component.text("§7死亡回数が上限に達しました！")
        val title = Title.title(mainTitle, subTitle, Title.Times.times(
            Duration.ofSeconds(2),
            Duration.ofSeconds(4),
            Duration.ofSeconds(1)
        ))

        // ゲームモード変更とタイトル表示
        player.gameMode = GameMode.SPECTATOR
        player.showTitle(title)
        player.sendMessage(Component.text("§e§lゲームオーバーです！観戦モードに移行しました。"))

        player.playSound(player.location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f)
    }
}