package dev.kuwa.siegePVP.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import org.bukkit.entity.Player
import com.github.puregero.multilib.MultiLib
import dev.kuwa.siegePVP.SiegePVP
import dev.kuwa.siegePVP.core.border.BorderManager
import dev.kuwa.siegePVP.core.game.GameManager
import dev.kuwa.siegePVP.core.player.PlayerManager
import dev.kuwa.siegePVP.core.team.TeamManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

@CommandAlias("siegedev|dev")
@Description("SiegePVP 開発用デバッグコマンド")
class SiegeDevCommand(
    private val plugin: SiegePVP,
    private val borderManager: BorderManager,
    private val teamManager: TeamManager,
    private val playerManager: PlayerManager,
    private val gameManager: GameManager
) : BaseCommand() {
    /**
     * GameManager デバッグ
     */
    @Subcommand("game state")
    @Description("現在のゲーム状態を確認します")
    fun onGameState(player: Player) {
        gameManager.getState()?.thenAccept { state ->
            player.sendMessage("${plugin.PREFIX}§e現在のGameState: §b$state")
        }
    }

    @Subcommand("game start")
    @Description("ゲームを強制開始します")
    fun onGameStart(player: Player) {
        gameManager.startGame()
        player.sendMessage("${plugin.PREFIX}§aゲームを強制開始しました")
    }

    @Subcommand("game stop")
    @Description("ゲームを強制停止します")
    fun onGameStop(player: Player) {
        gameManager.stopGame()
        player.sendMessage("${plugin.PREFIX}§cゲームを停止しました")
    }

    @Subcommand("game")
    fun onGameHelp(player: Player) {
        player.sendMessage("${plugin.PREFIX}§eGameManager デバッグ")
        player.sendMessage("§7/dev game state  §8- 現在の状態確認")
        player.sendMessage("§7/dev game start  §8- 強制開始")
        player.sendMessage("§7/dev game stop   §8- 強制停止")
    }

    /**
     * PlayerManager デバッグ
     */
    @Subcommand("player deathcount")
    @Syntax("<player>")
    @Description("プレイヤーの死亡回数を確認します")
    fun onCheckDeathCount(
        sender: Player,
        target: Player
    ) {
        playerManager.getPlayerDeathCount(target.uniqueId)
            ?.thenAccept { count ->
                sender.sendMessage("${plugin.PREFIX}§f${target.name} の死亡回数: §b$count")
            }
    }

    @Subcommand("player resetdeath")
    @Syntax("<player>")
    @Description("プレイヤーの死亡回数をリセットします")
    fun onResetDeath(
        sender: Player,
        target: Player
    ) {
        playerManager.resetPlayerDeathCount(target.uniqueId)
        sender.sendMessage("${plugin.PREFIX}§a${target.name} の死亡回数をリセットしました")
    }

    @Subcommand("player resetall")
    @Description("全プレイヤーの死亡回数をリセットします")
    fun onResetAll(sender: Player) {
        MultiLib.getAllOnlinePlayers().forEach {
            playerManager.resetPlayerDeathCount(it.uniqueId)
        }
        sender.sendMessage("${plugin.PREFIX}§a全プレイヤーの死亡回数をリセットしました")
    }

    @Subcommand("player")
    fun onPlayerHelp(player: Player) {
        player.sendMessage("${plugin.PREFIX}§ePlayerManager デバッグ")
        player.sendMessage("§7/dev player deathcount <player>  §8- 死亡回数確認")
        player.sendMessage("§7/dev player resetdeath <player>  §8- 個別リセット")
        player.sendMessage("§7/dev player resetall             §8- 全員リセット")
    }


    /**
     * Tag デバッグ
     */
    @Subcommand("tag get")
    @CommandCompletion("@players")
    fun onCheckTag(sender: org.bukkit.command.CommandSender, targetName: String) {
        val target: Player? = Bukkit.getPlayerExact(targetName)

        if (target == null) {
            sender.sendMessage("§cプレイヤー '$targetName' が見つかりません。")
            return
        }

        val tags = target.scoreboardTags
        if (tags.isEmpty()) {
            sender.sendMessage("§e${target.name} のタグはありません")
        } else {
            sender.sendMessage("§a${target.name} のタグ一覧:")
            tags.forEach { tag ->
                sender.sendMessage(" §f- $tag")
            }
        }
    }

    @Subcommand("tag add")
    @CommandCompletion("@players @nothing") // @nothing でタグ文字列補完なし
    @Syntax("<player> <tag>")
    @Description("指定したプレイヤーにタグを追加します")
    fun onAddTag(player: Player, sender: CommandSender, targetName: String, tag: String) {
        val target: Player? = Bukkit.getPlayerExact(targetName)

        if (target == null) {
            sender.sendMessage("§cプレイヤー '$targetName' が見つかりません")
            return
        }

        // 既にタグがあれば重複を防ぐ
        val tags = target.scoreboardTags
        if (tags.contains(tag)) {
            sender.sendMessage("§e${target.name} は既にタグ §f$tag §eを持っています")
            return
        }

        target.addScoreboardTag(tag)
        // MultiPaperのバグなのか、タグを付けたり消したりしても
        // 別インスタンスに反映されなかったり、再接続後に元に戻る不具合がるためこれで上書き
        MultiLib.chatOnOtherServers(player, "/tag $targetName add $tag")
        sender.sendMessage("§a${target.name} にタグ §f$tag §aを追加しました")
    }

    @Subcommand("tag remove")
    @CommandCompletion("@players @nothing") // タグは補完なし
    @Syntax("<player> <tag>")
    @Description("指定したプレイヤーからタグを削除します")
    fun onRemoveTag(player: Player, sender: CommandSender, targetName: String, tag: String) {
        val target: Player? = Bukkit.getPlayerExact(targetName)

        if (target == null) {
            sender.sendMessage("§cプレイヤー '$targetName' が見つかりません")
            return
        }

        val tags = target.scoreboardTags
        if (!tags.contains(tag)) {
            sender.sendMessage("§e${target.name} はタグ §f$tag §eを持っていません")
            return
        }

        target.removeScoreboardTag(tag)
        MultiLib.chatOnOtherServers(player, "/tag $targetName remove $tag")
        sender.sendMessage("§a${target.name} からタグ §f$tag §aを削除しました")
    }



    /**
     * 既存 DataStorage / Notify 系
     */
    @Subcommand("check datastorage")
    @Description("Global DataStorage の内容を全表示します")
    fun onCheckDataStorage(sender: CommandSender) {
        val prefix = ""
        val dataStorage = MultiLib.getDataStorage()
        sender.sendMessage("${plugin.PREFIX}§eDataStorage を読み込み中...")
        val future = dataStorage.list(prefix)
        future.thenAccept { map ->
            if (map.isEmpty()) {
                sender.sendMessage("${plugin.PREFIX}§cデータは存在しません")
                return@thenAccept
            }
            sender.sendMessage("${plugin.PREFIX}§a--- DataStorage 一覧 ---")
            map.toSortedMap().forEach { (key, value) ->
                sender.sendMessage("§7・§f$key §7= §b$value")
            }
        }
    }

    @Subcommand("localcheck")
    @CommandPermission("siege.localcheck")
    @Description("プレイヤーがローカルサーバー所属か確認します")
    fun onLocalCheck(player: Player) {
        val local = MultiLib.isLocalPlayer(player)
        player.sendMessage("${plugin.PREFIX}§fLocal Player ?: §b$local")
    }

    @Subcommand("setdata")
    @Syntax("<key> <value>")
    @Description("プレイヤーの PersistentData を設定します")
    fun onSetData(
        player: Player,
        @Name("key") key: String,
        @Name("value") value: String
    ) {
        MultiLib.setPersistentData(player, key, value)
        player.sendMessage("${plugin.PREFIX}§a保存しました: §f$key §7= §b$value")
    }

    @Subcommand("getdata")
    @Syntax("<key>")
    @Description("プレイヤーの PersistentData を取得します")
    fun onGetData(player: Player, @Name("key") key: String) {
        val value = MultiLib.getPersistentData(player, key) ?: "null"
        player.sendMessage("${plugin.PREFIX}§f$key §7= §b$value")
    }

    @Subcommand("global set")
    @Syntax("<key> <value>")
    @Description("Global DataStorage に値を保存します")
    fun onGlobalSet(player: Player, key: String, value: String) {
        MultiLib.getDataStorage().set(key, value).thenAccept {
            player.sendMessage("${plugin.PREFIX}§aGlobal保存: §f$key §7= §b$value")
        }
    }

    @Subcommand("global get")
    @Syntax("<key>")
    @Description("Global DataStorage から値を取得します")
    fun onGlobalGet(player: Player, key: String) {
        MultiLib.getDataStorage().get(key).thenAccept { value ->
            val msg = value ?: "null"
            player.sendMessage("${plugin.PREFIX}§f$key §7= §b$msg")
        }
    }

    @Subcommand("global list")
    @Syntax("<prefix>")
    @Description("Global DataStorage の一覧を取得します")
    fun onGlobalList(player: Player, prefix: String) {
        MultiLib.getDataStorage().list(prefix).thenAccept { map ->
            if (map.isEmpty()) {
                player.sendMessage("${plugin.PREFIX}§c該当キーはありません (prefix: $prefix)")
                return@thenAccept
            }
            player.sendMessage("${plugin.PREFIX}§a--- Global一覧 (prefix: $prefix) ---")
            map.toSortedMap().forEach { (k, v) ->
                player.sendMessage("§7・§f$k §7= §b$v")
            }
        }
    }

    @Subcommand("global")
    fun onGlobalHelp(player: Player) {
        player.sendMessage("${plugin.PREFIX}§eGlobal DataStorage 操作")
        player.sendMessage("§7/dev global set <key> <value>  §8- 値を保存")
        player.sendMessage("§7/dev global get <key>         §8- 値を取得")
        player.sendMessage("§7/dev global list <prefix>     §8- 一覧")
    }


    @Default
    fun help(player: Player) {
        player.sendMessage("${plugin.PREFIX}§e=== SiegePVP 開発用デバッグヘルプ ===")

        // GameManager
        player.sendMessage("§7/dev game state   §8- 現在のゲーム状態を確認")
        player.sendMessage("§7/dev game start   §8- ゲームを強制開始")
        player.sendMessage("§7/dev game stop    §8- ゲームを強制停止")

        // PlayerManager
        player.sendMessage("§7/dev player deathcount <player>  §8- プレイヤーの死亡回数確認")
        player.sendMessage("§7/dev player resetdeath <player>  §8- プレイヤーの死亡回数リセット")
        player.sendMessage("§7/dev player resetall             §8- 全プレイヤーの死亡回数リセット")

        // Tag
        player.sendMessage("§7/dev tag get <player>          §8- プレイヤーのタグ一覧表示")
        player.sendMessage("§7/dev tag add <player> <tag>    §8- プレイヤーのタグ一覧表示")
        player.sendMessage("§7/dev tag remove <player> <tag> §8- プレイヤーのタグ一覧表示")

        // DataStorage / Persistent
        player.sendMessage("§7/dev check datastorage           §8- Global DataStorage 全表示")
        player.sendMessage("§7/dev localcheck                  §8- ローカルサーバー所属確認")
        player.sendMessage("§7/dev setdata <key> <value>       §8- PersistentData 設定")
        player.sendMessage("§7/dev getdata <key>               §8- PersistentData 取得")

        // Global DataStorage
        player.sendMessage("§7/dev global set <key> <value>    §8- Global DataStorage 保存")
        player.sendMessage("§7/dev global get <key>            §8- Global DataStorage 取得")
        player.sendMessage("§7/dev global list <prefix>        §8- Global DataStorage 一覧")

        player.sendMessage("${plugin.PREFIX}§e=== End ===")
    }

}
