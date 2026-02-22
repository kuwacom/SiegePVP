package dev.kuwa.siegePVP.core.team

import com.github.puregero.multilib.DataStorageImpl
import com.github.puregero.multilib.MultiLib
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import org.bukkit.scoreboard.Team.Option
import org.bukkit.scoreboard.Team.OptionStatus
import java.util.concurrent.CompletableFuture

class TeamManager(
    private val plugin: Plugin,
    private val dataStorage: DataStorageImpl,
) {
    private val scoreboard: Scoreboard = Bukkit.getScoreboardManager().mainScoreboard

    fun createTeam() {
        removeAllTeams()

        scoreboard.registerNewTeam("red").apply {
            color(NamedTextColor.RED)
            setAllowFriendlyFire(false)
            setCanSeeFriendlyInvisibles(true)
            setOption(Option.COLLISION_RULE, OptionStatus.FOR_OTHER_TEAMS)
        }
        // teamRed.entries.forEach(teamRed::removeEntry)

        scoreboard.registerNewTeam("blue").apply {
            color(NamedTextColor.BLUE)
            setAllowFriendlyFire(false)
            setCanSeeFriendlyInvisibles(true)
            setOption(Option.COLLISION_RULE, OptionStatus.FOR_OTHER_TEAMS)
        }

    }

    fun removeAllTeams() {
        scoreboard.getTeam("red")?.unregister()
        scoreboard.getTeam("blue")?.unregister()
    }

    fun allocateTeams() {
        val teamRed = scoreboard.getTeam("red")
        val teamBlue = scoreboard.getTeam("blue")

        val players = MultiLib.getAllOnlinePlayers()

        // boss タグ付きプレイヤー
        val bossePlayers = players.filter { it.scoreboardTags.contains("boss") }

        if (bossePlayers.size >= 2) {
            teamRed?.addEntry(bossePlayers[0].name)
            bossePlayers[0].sendMessage("§cあなたは赤チームのボスです！")

            teamBlue?.addEntry(bossePlayers[1].name)
            bossePlayers[1].sendMessage("§9あなたは青チームのボスです！")
        }

        players.forEach { player -> player.scoreboard = scoreboard }

        // boss admin タグなし一般プレイヤー
        val normalPlayers = players
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

    /**
     * 新規チーム割り当て
     */
    fun allocateTeam(player: Player): Team? {
        player.scoreboard = scoreboard

        val teamRed = scoreboard.getTeam("red")
        val teamBlue = scoreboard.getTeam("blue")

        if (teamRed == null || teamBlue == null) return null

        // チーム人数をカウント
        val sizeRed = teamRed.entries.size
        val sizeBlue = teamBlue.entries.size

        // 人数が少ないチームに割り振り
        if (sizeRed <= sizeBlue) {
            teamRed.addEntry(player.name)

            player.sendMessage("§c§l赤チームに割り振られました！")
            val title = Title.title(
                Component.text("§c§lあなたは赤チームです！"),
                Component.text("")
            )
            player.showTitle(title)
            return teamRed
        } else {
            teamBlue.addEntry(player.name)

            player.sendMessage("§9§l青チームに割り振られました！")
            val title = Title.title(
                Component.text("§9§lあなたは青チームです！"),
                Component.text("")
            )
            player.showTitle(title)
            return teamBlue
        }
    }


    /**
     * すでにどちらかのチームに入っているかチェック
     */
    fun hasTeam(player: Player): Boolean =
        scoreboard.getEntryTeam(player.name) != null

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

    fun setTeamSpawnLocation(teamName: String, location: Location) {
        val sectionPath = "siege.TeamManager.teamSpawnLocation.$teamName"
        dataStorage
            .set("$sectionPath.world", location.world.name)
        dataStorage
            .set("$sectionPath.x", location.x)
        dataStorage
            .set("$sectionPath.y", location.y)
        dataStorage
            .set("$sectionPath.z", location.z)
        dataStorage
            .set("$sectionPath.yaw", location.yaw.toDouble())
        dataStorage
            .set("$sectionPath.pitch", location.pitch.toDouble())
    }

    fun getTeamSpawnLocation(teamName: String): CompletableFuture<Location?> {
        val sectionPath = "siege.TeamManager.teamSpawnLocation.$teamName"

        val worldFuture = dataStorage.get("$sectionPath.world")
        val xFuture = dataStorage.getDouble("$sectionPath.x")
        val yFuture = dataStorage.getDouble("$sectionPath.y")
        val zFuture = dataStorage.getDouble("$sectionPath.z")
        val yawFuture = dataStorage.getDouble("$sectionPath.yaw")
        val pitchFuture = dataStorage.getDouble("$sectionPath.pitch")

        return CompletableFuture.allOf(worldFuture, xFuture, yFuture, zFuture, yawFuture, pitchFuture)
            .thenApply {
                val worldName = worldFuture.getNow(null)
                val x = xFuture.getNow(null)
                val y = yFuture.getNow(null)
                val z = zFuture.getNow(null)
                val yaw = yawFuture.getNow(null)
                val pitch = pitchFuture.getNow(null)

                // どれか null なら null を返す
                if (worldName == null ||
                    x == null ||
                    y == null ||
                    z == null ||
                    yaw == null ||
                    pitch == null
                ) {
                    return@thenApply null
                }

                val world = Bukkit.getWorld(worldName) ?: return@thenApply null
                Location(world, x, y, z, yaw.toFloat(), pitch.toFloat())
            }
    }
}