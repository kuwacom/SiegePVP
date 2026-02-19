package dev.kuwa.siegePVP.core.team

import com.github.puregero.multilib.DataStorageImpl
import com.github.puregero.multilib.MultiLib
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import org.bukkit.scoreboard.Team.Option
import org.bukkit.scoreboard.Team.OptionStatus

class TeamManager(
    private val plugin: Plugin,
    private val dataStorage: DataStorageImpl,
) {
    private val board: Scoreboard = Bukkit.getScoreboardManager().mainScoreboard

    private var teamRed: Team? = null
    private var teamBlue: Team? = null

    fun createTeam() {
        teamRed = board.getTeam("red")
        teamBlue = board.getTeam("blue")
        removeAllTeams()

        teamRed = board.registerNewTeam("red").apply {
            color(NamedTextColor.RED)
            setAllowFriendlyFire(false)
            setCanSeeFriendlyInvisibles(true)
            setOption(Option.COLLISION_RULE, OptionStatus.FOR_OTHER_TEAMS)
        }
        // teamRed.entries.forEach(teamRed::removeEntry)

        teamBlue = board.registerNewTeam("blue").apply {
            color(NamedTextColor.BLUE)
            setAllowFriendlyFire(false)
            setCanSeeFriendlyInvisibles(true)
            setOption(Option.COLLISION_RULE, OptionStatus.FOR_OTHER_TEAMS)
        }

    }

    fun removeAllTeams() {
        teamRed = board.getTeam("red")
        teamBlue = board.getTeam("blue")
        teamRed?.unregister()
        teamBlue?.unregister()
    }

    fun allocateTeams() {
        if (teamRed == null || teamBlue == null) return

        // boss タグ付きプレイヤー
        val bossePlayers = MultiLib.getAllOnlinePlayers().filter { it.scoreboardTags.contains("boss") }

        if (bossePlayers.size >= 2) {
            teamRed?.addEntry(bossePlayers[0].name)
            bossePlayers[0].sendMessage("§cあなたは赤チームのボスです！")

            teamBlue?.addEntry(bossePlayers[1].name)
            bossePlayers[1].sendMessage("§9あなたは青チームのボスです！")
        }

        // boss タグなし一般プレイヤー
        val normalPlayers = MultiLib.getAllOnlinePlayers()
            .filter { !it.scoreboardTags.contains("boss") && !it.scoreboardTags.contains("admin") }
            .shuffled()

        normalPlayers.forEachIndexed { index, player ->
            if (index % 2 == 0) {
                teamRed?.addEntry(player.name)
                player.sendMessage("§c赤チームに入りました！")
            } else {
                teamBlue?.addEntry(player.name)
                player.sendMessage("§9青チームに入りました！")
            }
        }
    }

    fun allocateTeam(player: Player) {
        teamRed = board.getTeam("red")
        teamBlue = board.getTeam("blue")

        // すでにどちらかのチームに入っているかチェック
        val currentTeam = board.getEntryTeam(player.name)
        if (currentTeam != null) return

        // チーム人数をカウント
        val sizeRed = teamRed!!.entries.size
        val sizeBlue = teamBlue!!.entries.size

        // 人数が少ないチームに割り振り
        if (sizeRed <= sizeBlue) {
            teamRed?.addEntry(player.name)
            player.sendMessage("§c§l赤チームに割り振られました！")
            val title = Title.title(
                Component.text("§c§lあなたは赤チームです！"),
                Component.text("")
            )
            player.showTitle(title)
        } else {
            teamBlue?.addEntry(player.name)
            player.sendMessage("§9§l青チームに割り振られました！")
            val title = Title.title(
                Component.text("§9§lあなたは青チームです！"),
                Component.text("")
            )
            player.showTitle(title)
        }
    }

    fun countAliveSurvivalTeam(
        teamName: String?,
        allPlayers: Collection<Player>
    ): Int {
        if (teamName == null) return 0

        return allPlayers.count { p ->
            val pTeam = p.scoreboard.getEntryTeam(p.name)?.name

            // チームが同じ & サバイバルモード
            pTeam == teamName && p.gameMode == GameMode.SURVIVAL
        }
    }
}