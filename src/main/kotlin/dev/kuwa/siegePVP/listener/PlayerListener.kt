package dev.kuwa.siegePVP.listener

import com.github.puregero.multilib.MultiLib
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class PlayerListener: Listener {
    @EventHandler
    fun onPlayerMove(e: PlayerMoveEvent) {
        val player = e.player

        // 移動した位置のチャンクが local か external か
        val localChunk = MultiLib.isChunkLocal(player.location)
        val externalChunk = MultiLib.isChunkExternal(player.location)

        if (localChunk) {
            player.sendActionBar("このチャンクは local 所有です")
        } else if (externalChunk) {
            player.sendActionBar("このチャンクは external 所有です")
        }
    }
}