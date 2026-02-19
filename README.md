# SiegePVP

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue?logo=kotlin)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green?logo=minecraft)
![License](https://img.shields.io/badge/License-Custom-yellow)

MultiPaper対応の攻城戦PVP Minecraftプラグイン

## 概要

SiegePVPは、Minecraft 1.20.1向けの攻城戦PVPプラグインです  
MultiPaperに対応しており、複数のサーバーで動作する大規模攻城戦イベントを実現できます

## 対応バージョン

- Minecraft: `1.20.1`
- Paper API: `1.20.1-R0.1-SNAPSHOT`

## 主な機能

### ゲームシステム
- **ゲームステート管理**: 待機中 → 開始前 → 進行中 → 終了 の状態遷移
- **カウントダウン**: 試合開始前の5秒間カウントダウン
- **タイマー機能**: 試合時間の管理
- **ボス戦**: ボスプレイヤーの死亡で試合終了

### チームシステム
- **2チーム制**: 赤チーム、青チーム
- **ボス配置**: `boss`タグが付いたプレイヤー
- **自動チーム分配**: プレイヤーの均等な配分
- **フレンドリーファイア**: 無効化
- **衝突判定**: チーム間の衝突ルール無効化設定

### プレイヤー管理
- **死亡カウント**: プレイヤー死亡回数の記録 デフォルト4回
- **スコアボード**: リアルタイムなスコアボード表示 パケットレベルAPI
- **生存判定**: サバイバルモードでの生存判定 ゲームオーバーでスペクテイター

### マルチサーバー対応
- **MultiPaper**: サーバー間データ同期
- **グローバル通知**: 全サーバーへのタイトル表示

## 技術スタック

- **言語**: Kotlin 2.0.0
- **ビルドツール**: Gradle (Kotlin DSL)
- **API**: Paper API
- **ライブラリ**:
  - MultiPaper API (MultiLib)
  - Aikar Command Framework (acf-paper)
  - FastBoard

## プロジェクト構造

```
src/main/kotlin/dev/kuwa/siegePVP/
├── SiegePVP.kt              # メインプラグインクラス
├── commands/
│   ├── SiegePVPCommand.kt   # メインコマンド
│   └── SiegeDevCommand.kt   # デバッグ用コマンド(タグ付けは /dev tag を使うように)
├── core/
│   ├── game/
│   │   ├── GameManager.kt   # ゲーム管理
│   │   └── GameState.kt     # ゲーム状態列挙
│   ├── player/
│   │   └── PlayerManager.kt # プレイヤー管理
│   ├── task/
│   │   ├── Timer.kt         # タイマークラス
│   │   └── PlayerScoreboardUpdater.kt # スコアボード更新
│   └── team/
│       └── TeamManager.kt   # チーム管理
├── listener/
│   ├── PlayerDeathListener.kt # プレイヤ死亡リスナー
│   ├── PlayerJoin.kt        # プレイヤー参加リスナー
│   └── PlayerListener.kt    # 一般プレイヤーリスナー
└── utils/
    └── Utils.kt             # ユーティリティクラス
```

## ビルド方法

### 前提条件
- Java 17以上
- Gradle

### ビルド手順

```
bash
# ビルド実行
./gradlew build

# またはshadowJarを含むビルド
./gradlew shadowJar
```

生成されたjarファイルは `build/libs/` ディレクトリに保存されます

### 開発用サーバー起動

```
bash
./gradlew runServer
```

## 使用方法

### ゲーム開始

1. `/dev tag add <palyer> boss` 初めにボスを指定(チームに対して割り切れる数になるよう)
2. `/pvp team create` チームを作成
3. `/pvp team allocate` チーム割り振り

> ここからは各試合ごとに  
4. `/spawnpoint @a[team=red] ~ ~ ~` `/spawnpoint @a[team=blue] ~ ~ ~` でリス地固定
5. `/tp @a[team=red] ~ ~ ~` `/tp @a[team=blue] ~ ~ ~` で各自を所定値に飛ばす
6. `/pvp start` 開始 5秒カウントダウン始まる
7. `/gamemode survival @a` 事前にアドベンチャーにして先にスタートさせないようにしておく

### チーム参加

新規プレイヤーが途中参加すると、自動的にチームに分配されます
- 人数が少ないチームに自動配置
- ボスプレイヤーは専用タグで識別してるので、試合中に接続しても問題なし

### 試合終了条件

- ボスが死亡した場合
- 時間がなくなった場合

## コマンド

### SiegePVPCommand
メインのゲームコマンド（詳細実装を要確認）

### SiegeDevCommand
開発・テスト用コマンド

## 出典

- [PaperMC](https://papermc.io/) - Paper API
- [MultiPaper](https://github.com/puregero/multipaper)
- [MultiLib](https://github.com/MultiPaper/MultiLib) - MultiPaper API
- [Aikar](https://github.com/aikar/commands) - Command Framework
- [FastBoard](https://github.com/MrMicky-FR/FastBoard) - Packet Level ScoreBoard API
