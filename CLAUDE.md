# CLAUDE.md — 飲み会記録アプリ（Android / ヘルスコネクト連携）

このファイルは、このリポジトリで作業する Claude（および開発者）向けの設計ルールです。
実装・UI デザインの判断はまずここに従い、ここに無いことは「判断に迷ったら」の方針で決めてください。

---

## 1. プロダクト概要

飲み会の開始〜終了と、1杯ごとの飲酒量・時刻を記録し、ヘルスコネクトへ連携する Android アプリ。

管理する情報:

- 飲み会の開始時刻・終了時刻
- 飲んだお酒の種類・量・度数・時刻（1杯単位）
- 飲酒日 / 断酒日、連続断酒日数
- 推定血中アルコール濃度（BAC。飲んだ量と時刻から算出）

家飲みも「飲み会」として同じフローで記録する（家飲み専用のフローは作らない）。

---

## 2. 決定事項（変更する場合はユーザーに確認すること）

| # | 項目 | 決定 |
|---|---|---|
| 1 | 対応端末 | ヘルスコネクトの `AlcoholConsumptionRecord` が使える端末のみ。非対応端末は対象外 |
| 2 | 体重・性別 | 初回起動時に必須入力 |
| 3 | 日付の区切り | **AM 3:00 固定**（設定で変更不可） |
| 4 | 時刻指定での1杯追加 | なし。ボタン押下時刻で記録（後から履歴で編集は可） |
| 5 | 終了後の編集・削除 | できる。ヘルスコネクトにも反映 |
| 6 | 家飲み | 飲み会と同じ扱い |
| 7 | 常駐通知 | スワイプで消せてよい（Foreground Service は使わない） |
| 8 | 飲みすぎ警告・ペース通知 | MVP ではなし |
| 9 | 断酒日の確定 | 日付が変わった時点で、前日に飲酒記録がなければ断酒日 |
| 10 | 他アプリの飲酒記録 | 読み込まない・反映しない |

---

## 3. 技術スタック

| 項目 | 採用 |
|---|---|
| 言語 / UI | Kotlin, Jetpack Compose, Material 3 |
| アーキテクチャ | MVVM + UseCase + Repository, Hilt |
| 非同期 | Kotlin Coroutines / Flow |
| ローカル DB | Room |
| 設定 | DataStore (Preferences) |
| バックグラウンド | ongoing 通知 + BroadcastReceiver + WorkManager |
| 健康データ | Health Connect（`AlcoholConsumptionRecord` 書き込み、`WeightRecord` 読み取り） |
| グラフ | Compose 対応チャートライブラリ（例: Vico） |
| テスト | JUnit5 / kotlinx-coroutines-test / Turbine / Room in-memory DB / Compose UI Test |

- minSdk 34、targetSdk / compileSdk は最新
- Android 14〜16 は Health Connect の U Extensions 21 以上で `AlcoholConsumptionRecord` が使える。Android 17（API 37）以降は標準で使える
- 起動時に記録型が使えるかを判定し、使えなければ **「この端末は非対応です」画面を出して先へ進めない**

### 実装前に必ず確認すること（未検証事項）

- `AlcoholConsumptionRecord` の権限名の正式な表記（読み取り / 書き込み）
- 杯数（サービング数）を持つフィールドがあるか
- Jetpack `androidx.health.connect:connect-client` がこの記録型に対応した版。未対応ならプラットフォーム API（`android.health.connect`）を直接使う
- 記録型が使えるかの判定方法（`SdkExtensions.getExtensionVersion` / Health Connect の feature 判定 API）

参考: https://developer.android.com/reference/android/health/connect/datatypes/AlcoholConsumptionRecord

---

## 4. 状態遷移

アプリは3つの状態を循環する。**状態の正は常に Room**（メモリやサービスに持たない）。

```
[飲み会開始前] --「乾杯！」--> [飲み会中] --「お開き」--> [飲み会終了(サマリ)] --「閉じる」--> [飲み会開始前]
                                  ↺ ドリンクボタン（画面 or 通知）で1杯記録
```

| 遷移 | きっかけ | 処理 |
|---|---|---|
| 開始前 → 飲み会中 | 「乾杯！」ボタン | `party_session` 作成（`started_at` = 現在時刻）、常駐通知を表示 |
| 飲み会中 → 飲み会中 | ドリンクボタン（画面 / 通知アクション） | `drink_log` 追加、通知と推定 BAC を更新 |
| 飲み会中 → 終了 | 「お開き」ボタン（確認ダイアログあり） | `ended_at` 記録、通知消去、`HealthSyncWorker` を予約 |
| 終了 → 開始前 | サマリの「閉じる」 | DataStore の `lastFinishedSessionId` をクリア |

状態の導出:

- `ended_at IS NULL` の `party_session` がある → **飲み会中**（この行は常に最大1件）
- それが無く `lastFinishedSessionId` がある → **飲み会終了（サマリ表示）**
- どちらも無い → **飲み会開始前**

アプリの強制終了・端末再起動でも、上の導出で状態を復元する。`BOOT_COMPLETED` で飲み会中なら通知を出し直す。

お開き忘れ対策: 最後の1杯から 6 時間たったら「お開きしますか？」通知を出す。終了時刻の初期値は最後の1杯の時刻。

---

## 5. 画面

### 画面設計資料（デザインハンドオフ）

画面の見た目・操作は `doc/design_handoff_alcohol_meter/` を正とする。UI を実装・変更する前に必ず読むこと。

| ファイル | 内容 |
|---|---|
| `README.md` | 画面仕様本体。デザイントークン（色・タイポ・角丸・余白）、共通部品、全画面のレイアウトと寸法、操作時の挙動 |
| `alcohol-meter_v2.dc.html` | HTML のデザインモック（ブラウザで開くと全画面が並ぶ）。見た目の参照用で、コードは流用しない。表示には同フォルダの `support.js` と `_ds/` が必要 |
| `reference/original_spec.md` | ハンドオフ作成時点の CLAUDE.md のコピー |

- 優先順位: **見た目と操作はハンドオフの README.md、ロジック・データ・バックグラウンド方式・決定事項（§2）はこの CLAUDE.md** を優先する
- モックは幅 360dp 基準、1px = 1dp（フォントは sp）として読む。High-fidelity なので値はそのまま使う
- 飲み会中画面は **1b「もう 1 杯」優先** 案を実装する（キャンバス上の 1a / 1c は不採用の比較案）
- ハンドオフ内の記述と本ファイルの食い違い（本ファイルを優先）:
  - 常駐通知を「フォアグラウンドサービスの通知」と書いている箇所 → §2-7・§7 のとおり Foreground Service は使わない
  - README 末尾のファイル名 `nomikai_app_v2.dc.html` → 実ファイルは `alcohol-meter_v2.dc.html`

ホームは状態によって中身が切り替わる1画面。下部タブで「ホーム / 履歴 / カレンダー / 設定」。

| 画面 | 主な要素 | 備考 |
|---|---|---|
| ホーム（開始前） | 「乾杯！」ボタン、連続断酒日数、直近の飲み会 | 前回のアルコールが残っていれば推定 BAC も表示 |
| ホーム（飲み会中） | 経過時間タイマー、ドリンクボタン一覧、杯数・純アルコール量の合計、現在の推定 BAC、「お開き」ボタン | 1杯記録後に「取り消し」スナックバーを出す |
| サマリ | 開始・終了時刻、所要時間、時系列の杯リスト、種類別杯数、純アルコール量合計、BAC 推移グラフ、推定しらふ時刻、ヘルスコネクト連携状態 | 「閉じる」で開始前へ |
| 履歴一覧 | 過去の飲み会（日付・時間・杯数・純アルコール量）新しい順 | タップで詳細 |
| 履歴詳細 | サマリと同じレイアウト + 編集 | 杯の時刻・種類の編集、杯 / 飲み会の削除 |
| カレンダー | 月表示で飲酒日・断酒日を色分け、連続断酒日数、最長記録、月の断酒日数 | |
| 設定 | ドリンクプリセット編集（名前・種類・量 ml・度数 %・並び順・通知に出すか）、体重・性別、BAC 分解速度、ヘルスコネクト権限 | |
| オンボーディング | 端末対応チェック → 体重・性別入力 → ヘルスコネクト権限 → 通知権限 | 体重は `WeightRecord` の最新値を初期値に |
| 非対応端末 | 非対応の説明のみ | 先へ進めない |

初期ドリンクプリセット:

| 名前 | 量 | 度数 | ヘルスコネクト種別 |
|---|---|---|---|
| ビール（中ジョッキ） | 350 ml | 5% | BEER |
| ハイボール | 350 ml | 7% | HIGHBALL |
| 日本酒（1合） | 180 ml | 15% | SAKE |
| ワイン（グラス） | 120 ml | 12% | WINE |
| レモンサワー | 350 ml | 5% | CHUHAI |
| 焼酎ロック | 60 ml | 25% | SHOCHU |

---

## 6. UI / UX デザイン方針

具体的な色・寸法・文言は §5 の画面設計資料に従う。ここでは判断の拠り所となる方針を示す。

利用シーンは「飲み会の席で、酔いながら、片手で、暗い店内で」。これを最優先に設計する。

- **押しやすさ**: ドリンクボタンは大きく（最低 72dp 角目安）、画面下半分に配置して片手で押せるようにする。2×N のグリッド
- **誤操作への強さ**: 1杯記録は1タップで確定し、直後に「取り消し」スナックバー（約 5 秒）を出す。「お開き」は確認ダイアログを挟む。破壊的な操作は離れた位置に置く
- **押した手応え**: ボタン押下時に触覚フィードバックと、杯数カウンターのアニメーションを入れる
- **一目でわかる**: 飲み会中の画面は「経過時間」「杯数」「推定 BAC」の3つを大きく表示し、それ以外は控えめにする
- **暗所対応**: ダークテーマを第一に設計する（ライトテーマも対応）。Dynamic Color は使ってよいが、ドリンクボタンはプリセットの色で区別できるようにする
- **アクセシビリティ**: コントラスト比 4.5:1 以上、タップ領域 48dp 以上、全ボタンに contentDescription、フォント拡大 200% で崩れないこと
- **BAC の表示**: 必ず「推定値です。運転してよいかの判断には使えません」と併記する。数値は小数第 3 位まで（例: 0.045%）。しらふ時刻は「目安」と書く
- **断酒の見せ方**: 責めない・煽らないトーン。連続断酒日数は前向きな表現で見せる（「○日連続 休肝日」など）
- **通知**: タイトルに経過時間（`Chronometer`）、本文に杯数と推定 BAC、アクションにドリンクボタンを最大3つ

---

## 7. アーキテクチャ

```
ui/            Compose 画面 + ViewModel（StateFlow<UiState> を公開、イベントは関数で受ける）
domain/        UseCase と純粋関数（BacCalculator, AbstinenceCalculator, PureAlcohol）
data/
  local/       Room（Entity, Dao, Database）, DataStore
  health/      HealthConnectRepository（書き込み・体重読み取り・対応判定）
  repository/  PartyRepository, SettingsRepository
background/    PartyNotificationController, DrinkActionReceiver, BootReceiver,
               HealthSyncWorker, BacUpdateWorker, ForgotToEndWorker
di/            Hilt モジュール
```

ルール:

- ViewModel / Receiver / Worker はすべて同じ Repository・UseCase を通す（ロジックを重複させない）
- 計算ロジック（BAC・断酒日数・純アルコール量）は Android に依存しない純粋関数にし、単体テストを書く
- 時刻は `Clock` を注入する（テストで固定できるように）
- 現在時刻を `System.currentTimeMillis()` や `Instant.now()` で直接取らない

### バックグラウンド動作（方式A・確定）

- 常駐プロセス・Foreground Service は使わない
- 経過時間は通知の `Chronometer`（`setUsesChronometer(true)` + `setWhen(startedAt)`）で OS に描画させる
- 通知アクション → `DrinkActionReceiver` → `drink_log` 追加 → 通知更新
- BAC 表示の更新は「1杯記録時」と「`BacUpdateWorker`（15 分ごと、飲み会中のみ）」
- ユーザーが通知を消しても飲み会は続く。アプリを開いたら通知を出し直す
- 権限: `POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`、Health Connect の権限

---

## 8. データモデル（Room）

時刻はすべて UTC の `Instant` + 記録時の `ZoneOffset` の組で保存する。
飲まなかった日・断酒日数は保存せず、`drink_log` から毎回計算する。

### party_session

| カラム | 型 | 内容 |
|---|---|---|
| id | String (UUID) | 主キー |
| started_at / start_offset | Instant / ZoneOffset | 開始時刻 |
| ended_at / end_offset | Instant? / ZoneOffset? | 終了時刻。null なら飲み会中 |
| title | String? | 任意のメモ |
| hc_sync_status | Enum | NOT_SYNCED / SYNCED / FAILED |

### drink_log

| カラム | 型 | 内容 |
|---|---|---|
| id | String (UUID) | 主キー。ヘルスコネクトの `clientRecordId` にも使う |
| session_id | String | `party_session` への外部キー（CASCADE 削除） |
| drank_at / offset | Instant / ZoneOffset | 飲んだ時刻（ボタン押下時刻。履歴から編集可） |
| name | String | プリセット名のコピー（プリセットを後で変えても履歴は変えない） |
| beverage_type | Int | ヘルスコネクトの飲料種別定数 |
| volume_ml | Double | 1杯の量 |
| abv_percent | Double | 度数 |
| pure_alcohol_g | Double | 純アルコール量（保存時に計算） |
| hc_record_id | String? | ヘルスコネクト側 ID |

### drink_preset

| カラム | 型 | 内容 |
|---|---|---|
| id | Long | 主キー |
| name / beverage_type / volume_ml / abv_percent | | drink_log と同じ意味 |
| icon / color | String | ボタンの見た目 |
| sort_order | Int | 並び順 |
| show_in_notification | Boolean | 通知アクションに出すか（最大3件） |

### DataStore

体重 kg、性別、BAC 分解速度 β（初期値 0.015 %/時）、記録開始日、`lastFinishedSessionId`、オンボーディング完了フラグ。

---

## 9. 計算ロジック

### 純アルコール量

```
A[g] = 量[ml] × 度数[%] / 100 × 0.8
```

### 推定血中アルコール濃度（Widmark 式）

```
1杯あたりの上昇分 ΔBAC[%] = A[g] / (体重[kg] × r × 10)
r = 0.68（男性） / 0.55（女性）
分解速度 β = 0.015 %/時（設定で変更可）
```

1. 飲酒ログを時刻順に並べる
2. 各杯の時刻で ΔBAC を足す
3. 次の杯までの経過時間 × β を引き、0 未満は 0 にする
4. 最後の杯以降は「現在の BAC ÷ β」で推定しらふ時刻（BAC が 0 になる時刻）を出す

- グラフ用は 5 分刻みで同じ計算をする（最初の杯 → しらふ時刻）
- 実装は `BacCalculator`（純粋関数）。境界値（0 杯、1 杯、長時間の間隔、BAC が 0 に戻った後の追加）をテストする

### 断酒日数

- 「論理日付」= ローカル時刻から 3 時間引いた日付（AM 3:00 区切り）
- 飲酒日 = `drink_log` が1件以上ある論理日付
- 断酒日 = 記録開始日以降で、飲酒日ではなく、**すでに終わった**論理日付（今日は数えない）
- 連続断酒日数 = 今日の論理日付 − 最後の飲酒日 − 1（飲酒記録が無ければ記録開始日から数える）
- 最長断酒日数 = 飲酒日どうしの間隔の最大値（記録開始日 → 最初の飲酒日も含む）
- 月間集計 = 飲酒日数、断酒日数、純アルコール量の合計・1日平均
- 実装は `AbstinenceCalculator`（純粋関数）。AM 2:59 / 3:00 の境界、月またぎ、タイムゾーン変更をテストする

---

## 10. ヘルスコネクト連携

お開き時に、その飲み会の `drink_log` を1杯 = 1レコードの `AlcoholConsumptionRecord` として書き込む。

| ヘルスコネクトの項目 | アプリの値 |
|---|---|
| 時刻 | `drank_at`（瞬間として記録。開始 = 終了） |
| beverageType | プリセットの種別 |
| servingVolume | `volume_ml` |
| alcoholByVolume | `abv_percent` |
| notes | プリセット名 |
| metadata.clientRecordId | `drink_log.id`（再送しても重複しない） |
| metadata.recordingMethod | 手動入力 |

飲料種別の対応: ビール → BEER、ハイボール → HIGHBALL、日本酒 → SAKE、焼酎 → SHOCHU、ワイン → WINE、レモンサワー・チューハイ → CHUHAI、カクテル → COCKTAIL、ウイスキー → WHISKEY、それ以外 → OTHER。

流れ:

1. 起動時に Health Connect の利用可否と記録型の対応を確認（非対応なら非対応画面）
2. 書き込み権限（+ 体重の読み取り権限）を要求
3. お開きで `HealthSyncWorker` を起動し、未連携の `drink_log` をまとめて書き込む
4. 失敗したら WorkManager の指数バックオフで再試行。サマリ画面に連携状態を表示
5. 履歴を編集・削除したら、`clientRecordId` で同じレコードを更新・削除する

- 飲み会そのもの（開始・終了時刻）を表す記録型は無いので、アプリ内だけで管理する
- 断酒日は何も書き込まない（記録が無い日 = 飲んでいない日）
- 他アプリが書いた飲酒記録は読まない

---

## 11. 開発ルール

- コードのコメント・コミットメッセージは日本語で可。識別子は英語
- UI 文言は `strings.xml` に置く（ハードコードしない）
- 新しい画面には Compose Preview（ライト / ダーク / フォント拡大）を付ける
- 計算ロジック・Repository・状態導出には単体テストを書く
- Room のスキーマ変更時は Migration と `exportSchema` を必ず更新する
- 個人の健康データを扱うため、ログに体重・飲酒量を出さない。Analytics 等の外部送信は入れない

### よく使うコマンド

```bash
./gradlew assembleDebug          # ビルド
./gradlew testDebugUnitTest      # 単体テスト
./gradlew connectedDebugAndroidTest  # 実機 / エミュレータでのテスト
./gradlew lint                   # Lint
```

---

## 12. 判断に迷ったら

1. 「酔っていても間違えずに使えるか」を優先する
2. 状態は Room から導出する。新しいフラグを増やす前に、既存データから導けないか考える
3. 決定事項（§2）と矛盾する変更や、ヘルスコネクトに書き込むデータの形を変える変更は、実装前にユーザーに確認する
4. MVP の範囲外（警告通知、時刻指定の追加、他アプリ連携など）は実装せず、TODO として提案に留める
