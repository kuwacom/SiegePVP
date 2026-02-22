package dev.kuwa.siegePVP.core.game

import com.github.puregero.multilib.DataStorageImpl
import com.github.puregero.multilib.MultiLib
import dev.kuwa.siegePVP.core.player.PlayerManager
import dev.kuwa.siegePVP.core.task.Timer
import dev.kuwa.siegePVP.core.task.PlayerScoreboardUpdater
import dev.kuwa.siegePVP.core.team.TeamManager
import dev.kuwa.siegePVP.utils.clearAllDroppedItems
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title.Times
import net.kyori.adventure.title.Title.title
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.time.Duration
import java.util.concurrent.CompletableFuture

class GameManager(
    private val plugin: Plugin,
    private val dataStorage: DataStorageImpl,
    private val teamManager: TeamManager,
    private val playerManager: PlayerManager
) {
    val timer = Timer(plugin) {
        onTimeUp()
        // 全サーバーのタイマーのうち一度だけ実行するようにする(stateは常にglobal state)
        getState()?.thenAccept { state ->
            if (state == GameState.RUNNING) stopGame()
        }
    }
    val playerScoreboardUpdater = PlayerScoreboardUpdater(plugin, this, teamManager, playerManager, timer)
    var countdownTask: BukkitTask? = null

    init {
        setState(GameState.WAITING)
    }

    fun getState(): CompletableFuture<GameState>? {
        return dataStorage.get("siege.GameManager.state")
            ?.thenApply { data ->
                val state = try {
                    GameState.valueOf(data)
                } catch (e: IllegalArgumentException) {
                    GameState.WAITING  // fallback Game State
                }
                state
            }
    }

    private fun setState(state: GameState) {
        dataStorage.set("siege.GameManager.state", state.name)
        MultiLib.notify("siege.GameManager.state", state.name)
    }

    fun startGame() {
        setState(GameState.STARTING)
        clearAllDroppedItems()

        for (player in MultiLib.getAllOnlinePlayers()) {
            // adminは飛ばす
            if (player.scoreboardTags.contains("admin")) continue

            // チームに入ってなかったら少ないほうのチームに追加する
            if (!teamManager.hasTeam(player))
                teamManager.allocateTeam(player)

            // デフォルトのゲームモードに変更
            player.gameMode = GameMode.ADVENTURE

            // チームのスポーンポイントへtp
            // スポーンポイントを変更してチームスポーンポイントへtp
            val team = player.scoreboard.getEntryTeam(player.name)
            team?.let {
                teamManager.getTeamSpawnLocation(it.name).thenAcceptAsync( { location ->
                    player.setBedSpawnLocation(location, true)
                    location?.let { player.teleport(it) }
                }, { task -> Bukkit.getScheduler().runTask(plugin, task) })
            }

            // プレイヤーの状態を全てリセット
            playerManager.resetPlayerState(player)
        }

        // 5秒前からのカウントダウン
        countdownTask = object : BukkitRunnable() {
            var seconds = 5

            override fun run() {
                if (seconds > 0) {
                    // カウントダウン中の処理 (音とメッセージ)
                    sendGlobalTitle("", "§e試合開始まであと §c$seconds §e秒...",
                        Times.times(
                            Duration.ofSeconds(0),
                            Duration.ofSeconds(4),
                            Duration.ofSeconds(0)
                        ))
                    MultiLib.getAllOnlinePlayers().forEach { player ->
                        player.sendMessage("§e試合開始まであと §c$seconds §e秒...")
                        // カウント音 (チッ、チッという音)
                        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f)
                    }
                    seconds--
                } else {
                    // 0秒になった時の処理(試合開始)
                    setState(GameState.RUNNING)

                    sendGlobalTitle("§6§l試合開始！", "")

                    MultiLib.getAllOnlinePlayers().forEach { player ->
                        player.sendMessage("§6§l試合開始！！")
                        // 開始音(オーボエの音などの派手な音)
//                        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                        player.playSound(player.location, Sound.ITEM_GOAT_HORN_SOUND_0, 1f, 1f)

                        // adminは飛ばす
                        if (player.scoreboardTags.contains("admin")) return@forEach
                        // 開始時にサバイバルにする
                        player.gameMode = GameMode.SURVIVAL
                    }

                    // サイドバー表示
                    playerScoreboardUpdater.start()
                    // timer始動
                    timer.start()

                    this.cancel() // タスクを終了
                }
            }
        }.runTaskTimer(plugin, 0L, 20L) // 0秒後から開始し、20ティック(1秒)ごとに実行

    }

    /**
     * ## notify で　`siege.GameManager` `STARTING`を受信した場合
     * startGame は呼ばれていない前提
     */
    fun onStartGame() {
        playerScoreboardUpdater.start()
        timer.start()
    }

    /**
     * 全てをリセット
     */
    fun stopGame() {
        setState(GameState.ENDED)

        MultiLib.getAllOnlinePlayers().forEach { player ->
            // adminは飛ばす
            if (player.scoreboardTags.contains("admin")) return@forEach
            // 初期ゲームモードに戻す
            player.gameMode = GameMode.ADVENTURE
            // プレイヤーのスポーンポイントを消す
            player.bedSpawnLocation = null
        }

        Bukkit.broadcast(Component.text("§e§lゲームが終了しました！"));

        // 利用しているカウントダウンタスクがあったら終了
        countdownTask?.cancel()

        // timerの停止
        timer.stop()
//        // サイドバー表示更新の停止及び非表示化
//        playerScoreboardUpdater.stop()
        // 死亡回数カウントのリセット
        playerManager.resetAllPlayerDeathCount()

        setState(GameState.WAITING)
    }
    /**
     * ## notify で　`siege.GameManager` `ENDED`を受信した場合
     * startGame は呼ばれていない前提
     */
    fun onStopGame() {
        Bukkit.broadcast(Component.text("§e§lゲームが終了しました！"));

        // timerの停止
        timer.stop()
//        // サイドバー表示更新の停止及び非表示化
//        playerScoreboardUpdater.stop()
    }


    /**
     * ボス死亡時
     * 固定2チームのみ前提の設計
     */
    fun onBossDeath(player: Player) {
        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        val team = scoreboard.getEntryTeam(player.name)
        val color = team?.color ?: ChatColor.RESET
        var title = "§l";

        if (team?.name == "red") {
            title += "§9青チームの勝！！"
        } else if (team?.name == "blue") {
            title += "§c赤チームの勝！！"
        }

        sendGlobalTitle(title, "§a§l試合終了！")
        MultiLib.getAllOnlinePlayers().forEach { player ->
            player.playSound(player.location, Sound.ITEM_GOAT_HORN_SOUND_5, 1f, 1f)
        }

        stopGame()
    }

    fun onTimeUp() {
        sendGlobalTitle("§e§lタイムアップ！！", "§a§l試合終了！")
        MultiLib.getAllOnlinePlayers().forEach { player ->
            player.playSound(player.location, Sound.ITEM_GOAT_HORN_SOUND_2, 1f, 1f)
        }
    }

    private fun sendGlobalTitle(title: String, subtitle: String, times: Times =             Times.times(
        Duration.ofSeconds(1),   // フェードイン
        Duration.ofSeconds(4),   // 表示時間
        Duration.ofSeconds(1)    // フェードアウト
    )) {
        val title = title(
            Component.text(title),
            Component.text(subtitle),
            times
        )

        val allPlayers = MultiLib.getAllOnlinePlayers()
        allPlayers.forEach {
            it.showTitle(title)
        }
    }
}