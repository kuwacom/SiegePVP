package dev.kuwa.siegePVP.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.github.puregero.multilib.DataStorageImpl
import com.github.puregero.multilib.MultiLib
import dev.kuwa.siegePVP.SiegePVP
import dev.kuwa.siegePVP.core.border.BorderManager
import dev.kuwa.siegePVP.core.game.GameManager
import dev.kuwa.siegePVP.core.player.PlayerManager
import dev.kuwa.siegePVP.core.team.TeamManager
import dev.kuwa.siegePVP.utils.resolveRelativeCoords
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("siegepvp|pvp")
class SiegePVPCommand(
    private val plugin: SiegePVP,
    private val borderManager: BorderManager,
    private val teamManager: TeamManager,
    private val playerManager: PlayerManager,
    private val gameManager: GameManager,
    private val dataStorage: DataStorageImpl
) : BaseCommand() {

    @Subcommand("start")
    fun onStart(sender: CommandSender) {
        gameManager.startGame()
    }

    @Subcommand("stop")
    fun onStop(sender: CommandSender) {
        gameManager.stopGame()
    }

    @Subcommand("config set gameOverDeathCount")
    @Syntax("<value>")
    @Description("プレイヤーの死亡上限数を設定します")
    fun onConfigSetGameOverDeathCount(
        player: Player,
        @Name("value") value: Int
    ) {
        playerManager.gameOverDeathCount = value
        MultiLib.notify(
            "siege:SiegePVPCommand/config set gameOverDeathCount",
            value.toString()
        )

        player.sendMessage("${plugin.PREFIX} §eプレイヤーの死亡上限数を §b$value §eに設定しました")
    }
    @Subcommand("config get gameOverDeathCount")
    fun onConfigGetGameOverDeathCount(player: Player) {
        player.sendMessage("${plugin.PREFIX} §eプレイヤーの死亡上限数は §b${playerManager.gameOverDeathCount} §eに設定されています")
    }

    @Subcommand("config set timer")
    @Syntax("<minutes>")
    @Description("プレイヤーの死亡上限数を設定します")
    fun onConfigSetTimer(
        player: Player,
        @Name("minutes") minutes: Long
    ) {
        gameManager.timer.duration = minutes * 60L
        MultiLib.notify(
            "siege:SiegePVPCommand/config set timer",
            minutes.toString()
        )

        player.sendMessage("${plugin.PREFIX} §eゲーム上限時間を §b$minutes §eに設定しました")
    }
    @Subcommand("config get timer")
    fun onConfigGetTimer(player: Player) {
        player.sendMessage("${plugin.PREFIX} §eゲーム上限時間は §b${gameManager.timer.duration} §eに設定されています")
    }

    @Subcommand("team create")
    fun onTeamCreate(player: Player) {
        teamManager.createTeam()
        player.sendMessage("${plugin.PREFIX} §eチームを作成しました！ §cred §9blue")
    }

    @Subcommand("team remove")
    fun onTeamRemove(player: Player) {
        teamManager.removeAllTeams()
        player.sendMessage("${plugin.PREFIX} §e全てのチームを削除しました！")
    }

    @Subcommand("team allocate")
    fun onTeamAllocate(player: Player) {
        teamManager.allocateTeams()
        player.sendMessage("${plugin.PREFIX} §e全てのプレイヤーをteamに割り振りました！")
    }


    /**
     * 各チームのリス地設定
     */
    @Subcommand("team spawnpoint")
    @Syntax("<teamName>")
    fun onTeamSpawnPoint(
        player: Player,
        @Name("teamName") teamName: String
    ) {
        val location = player.location

        // チームのスポーン地点の保存
        teamManager.setTeamSpawnLocation(teamName, location)

        player.sendMessage("${plugin.PREFIX} §aチーム §r$teamName §aのスポーンポイントを設定しました！")
    }

    @Subcommand("border set")
    @CommandCompletion("~|<x> ~|<z> <duration> [size]")
    fun onBorderSet(
        player: Player,
        @Name("x") xArg: String,
        @Name("z") zArg: String,
        @Name("duration") duration: Int,
        @Optional @Name("size") size: Double?
    ) {
        val world = player.world
        val borderManager = borderManager
        val border = world.worldBorder

        // 現在の中心
        val startX = border.center.x
        val startZ = border.center.z
        val startSize = border.size

        // ターゲット座標
        val (targetX, targetZ) = resolveRelativeCoords(player.location, xArg, zArg)

        // ターゲットサイズ（指定なければ現在のまま）
        val targetSize = size ?: startSize

        // duration -> ticks
        val ticks = duration.toLong() * 20L

        // 開始
        borderManager.start(
            world,
            startX,
            startZ,
            targetX,
            targetZ,
            startSize,
            targetSize,
            ticks
        )

        player.sendMessage(
            "${plugin.PREFIX} §aワールドボーダーを §r$duration §a秒かけて " +
                    "§rX:${targetX.toInt()} Z:${targetZ.toInt()} §aへ移動させ、 " +
                    "§r${targetSize.toInt()} §aのサイズに設定しました！"
        )
    }

    @Subcommand("border stop")
    fun onBorderStop(sender: Player) {
        if (borderManager.isRunning()) {
            borderManager.stop()
            sender.sendMessage("${plugin.PREFIX} §cワールドボーダーの移動を停止しました！")
        } else {
            sender.sendMessage("${plugin.PREFIX} §e現在進行中のワールドボーダー移動はありません！")
        }
    }


    @Subcommand("sidebar")
    @CommandCompletion("on|off")
    fun onSidebar(
        player: Player,
        @Name("mode") mode: String
    ) {
        if (mode.equals("on", true)) {
            gameManager.playerScoreboardUpdater.start()
            player.sendMessage("${plugin.PREFIX} §eプレイ情報表示を§aON§eにしました！")

            MultiLib.notify(
                "siege:SiegePVPCommand/sidebar",
                "on"
            )
        } else {
            gameManager.playerScoreboardUpdater.stop()
            player.sendMessage("${plugin.PREFIX} §eプレイ情報表示を§cOFF§eにしました！")

            MultiLib.notify(
                "siege:SiegePVPCommand/sidebar",
                "off"
            )
        }
    }

    @Subcommand("reset-spawnpoint all")
    fun onResetSpawnPointAll(sender: CommandSender) {
        MultiLib.getAllOnlinePlayers().forEach { player ->
            player.bedSpawnLocation = null
        }
        sender.sendMessage("§aすべてのプレイヤーのスポーン地点を削除しました")
    }
}
