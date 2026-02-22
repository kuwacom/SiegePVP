package dev.kuwa.siegePVP

import co.aikar.commands.PaperCommandManager
import com.github.puregero.multilib.DataStorageImpl
import org.bukkit.plugin.java.JavaPlugin

import com.github.puregero.multilib.MultiLib
import dev.kuwa.siegePVP.commands.SiegeDevCommand
import dev.kuwa.siegePVP.commands.SiegePVPCommand
import dev.kuwa.siegePVP.core.border.BorderManager
import dev.kuwa.siegePVP.core.game.GameManager
import dev.kuwa.siegePVP.core.game.GameState
import dev.kuwa.siegePVP.core.game.GameState.*
import dev.kuwa.siegePVP.core.player.PlayerManager
import dev.kuwa.siegePVP.core.team.TeamManager
import dev.kuwa.siegePVP.listener.PlayerDeathListener
import dev.kuwa.siegePVP.listener.PlayerJoin
import dev.kuwa.siegePVP.listener.PlayerListener

class SiegePVP : JavaPlugin() {
    lateinit var manager: PaperCommandManager

    lateinit var borderManager: BorderManager
    lateinit var teamManager: TeamManager
    lateinit var playerManager: PlayerManager
    lateinit var gameManager: GameManager

    // storage 準備（MultiLib）
    val dataStorage: DataStorageImpl = MultiLib.getDataStorage()

    val PREFIX = "§8[§bSiegePVP§8]§r"

    override fun onEnable() {
        // Plugin startup logic

        logger.info("==== SiegePVP ==== Example Enabled!!")

        // managers を生成
        borderManager = BorderManager(this)
        teamManager = TeamManager(this, dataStorage)
        playerManager = PlayerManager(this, dataStorage)
        gameManager = GameManager(this, dataStorage, teamManager, playerManager)

        registerCommands()
        registerListeners()
        registerMultiLibListeners()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    private fun registerCommands() {
        // CommandManager を生成
        manager = PaperCommandManager(this)

        // コマンドクラスを登録
        manager.registerCommand(SiegeDevCommand(this, borderManager, teamManager, playerManager, gameManager))
        manager.registerCommand(SiegePVPCommand(this, borderManager, teamManager, playerManager, gameManager, dataStorage))
    }

    private fun registerListeners() {
//        server.pluginManager.registerEvents(PlayerListener(), this)
        server.pluginManager.registerEvents(PlayerDeathListener(this ,gameManager, playerManager), this)
        server.pluginManager.registerEvents(PlayerJoin(this ,gameManager, teamManager, playerManager), this)
    }

    private fun registerMultiLibListeners() {
        // MultiLib
        // https://github.com/MultiPaper/MultiLib

        // グローバルデータ保存のコールバック登録
        MultiLib.onString(this, "example:chatdata") { data, reply ->
            logger.info(PREFIX + "Received cross-server data: $data")
            reply.accept("ok", "メッセージ受信")
        }

        /**
         * MultiLib の通知で String を受け取るチャンネルを登録
         */
        // Game の state を取得
        MultiLib.onString(this, "siege.GameManager.state") { data, _ ->
            logger.info("Received match start notification: $data")
            val newState = try {
                GameState.valueOf(data)
            } catch (e: IllegalArgumentException) {
                // 不正な値が来たときの fallback
                WAITING
            }
            when (newState) {
                WAITING -> {}
                STARTING -> {}
                RUNNING -> {
                    gameManager.onStartGame()
                }
                ENDED -> {
                    gameManager.onStopGame()
                }
            }
        }


        /**
         * SiegePVPCommand
         */
        MultiLib.onString(this, "siege.SiegePVPCommand/config set gameOverDeathCount") { data, _ ->
            playerManager.gameOverDeathCount = data.toInt()
        }
        MultiLib.onString(this, "siege.SiegePVPCommand/config set timer") { data, _ ->
            gameManager.timer.duration = data.toLong() * 60L
        }

        MultiLib.onString(this, "siege.SiegePVPCommand/sidebar") { data, _ ->
            if (data == "on") {
                gameManager.playerScoreboardUpdater.start()
            } else {
                gameManager.playerScoreboardUpdater.stop()
            }
        }
    }

}
