package dev.kuwa.siegePVP.utils

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
