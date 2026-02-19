package dev.kuwa.siegePVP.core.game

enum class GameState {
    WAITING, // 開始前
    STARTING, // 開始中
    RUNNING, // 開催中
    ENDED // 終了済み (すぐに waiting になる)
}