package dev.kuwa.siegePVP.utils

import org.bukkit.Location
import kotlin.time.Duration.Companion.seconds

/**
 * 秒を 00:00 形式に変換
 */
fun formatSeconds(seconds: Long): String {
    // 秒を Duration オブジェクトに変換
    val duration = seconds.seconds

    // 各コンポーネント（分、秒）に分解してフォーマット
    return duration.toComponents { minutes, secs, _ ->
        "%02d:%02d".format(minutes, secs)
    }
}

/**
 * チルダ対応の座標変換
 */
fun resolveRelativeCoords(
    base: Location,
    xStr: String,
    zStr: String
): Pair<Double, Double> {
    val x = if (xStr.startsWith("~")) {
        val offset = xStr.removePrefix("~").toDoubleOrNull() ?: 0.0
        base.x + offset
    } else {
        xStr.toDouble()
    }

    val z = if (zStr.startsWith("~")) {
        val offset = zStr.removePrefix("~").toDoubleOrNull() ?: 0.0
        base.z + offset
    } else {
        zStr.toDouble()
    }

    return Pair(x, z)
}