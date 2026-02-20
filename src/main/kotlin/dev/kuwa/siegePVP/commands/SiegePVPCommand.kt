package dev.kuwa.siegePVP.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.github.puregero.multilib.MultiLib
import dev.kuwa.siegePVP.SiegePVP
import dev.kuwa.siegePVP.core.game.GameManager
import dev.kuwa.siegePVP.core.player.PlayerManager
import dev.kuwa.siegePVP.core.team.TeamManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("siegepvp|pvp")
class SiegePVPCommand(
    private val plugin: SiegePVP,
    private val teamManager: TeamManager,
    private val playerManager: PlayerManager,
    private val gameManager: GameManager
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
     * 各チームの初期tpとリス地設定
     */
    @Subcommand("team tp")
    @Syntax("<teamName>")
    fun onTeamTP(
        player: Player,
        @Name("teamName") teamName: String
    ) {
        val location = player.location

        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        val team = scoreboard.getTeam(teamName) ?: return

        for (entry in team.entries) {
            val targetPlayer = Bukkit.getPlayerExact(entry) ?: continue
            targetPlayer.teleport(location)
            targetPlayer.setBedSpawnLocation(location, true)
        }

        player.sendMessage("${plugin.PREFIX} §aチーム §r$teamName §aを現在地へtpしスポーン地点を設定しました！")
    }

//    @Subcommand("side on")
//    fun onSideOn(player: Player) {
//        gameManager.playerScoreboardUpdater.start()
//        player.sendMessage("${plugin.PREFIX} §eプレイ情報表示を§aON§eにしました！")
//    }
//
//    @Subcommand("side off")
//    fun onSideOff(player: Player) {
//        gameManager.playerScoreboardUpdater.stop()
//        player.sendMessage("${plugin.PREFIX} §eプレイ情報表示を§cOFF§eにしました！")
//    }
}
