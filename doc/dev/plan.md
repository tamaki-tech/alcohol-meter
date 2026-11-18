# 開発計画（Claude → Codex 指示用の共有ドキュメント）

Claude（左ペイン）が全体を指揮し、2 体の Codex が並行で実装する。
仕様の正は `CLAUDE.md`（ロジック・データ・決定事項）と `doc/design_handoff_alcohol_meter/README.md`（見た目・操作）。**作業前に両方を必ず読むこと。**

## 担当

| 担当 | ペイン | 持ち場（このディレクトリ以外は原則触らない） |
|---|---|---|
| **Codex-A** | 右上 | Gradle 構成一式、`ui/`（テーマ・共通部品・全画面・ViewModel・Navigation）、`MainActivity`、`res/`（strings / fonts / drawable） |
| **Codex-B** | 右下 | `domain/`、`data/`、`background/`、`di/`、`AndroidManifest.xml` の権限・Receiver 宣言、Health Connect 調査 |

- 相手の持ち場のファイルを変更する必要が出たら、自分では変更せず、作業報告の「相手への依頼」に書く（Claude が中継する）。
- 例外: `AndroidManifest.xml` は A が雛形を作り、B が権限・Receiver・Health Connect 関連を追記する。`app/build.gradle.kts` / `libs.versions.toml` は A が持つが、B が依存を追加するのは可（追加した旨を報告）。
- **git commit / push はしない**（Claude が最終確認後にまとめてコミットする）。`git stash` / `git checkout -- .` / `git reset` など他人の作業を消す操作も禁止。

## 共通の取り決め

- パッケージ / applicationId: `tech.tamaki.alcoholmeter`、モジュールは `:app` 1 つ
- minSdk 34 / compileSdk・targetSdk はローカルにある最新（android-36。37 が必要なら `sdkmanager` で入れてよい）
- JDK 17、Gradle Wrapper 8.11 以上、Kotlin 2.x + Compose Compiler プラグイン、KSP（Room / Hilt）
- 時刻は `java.time.Clock` を Hilt で注入。`Instant.now()` / `System.currentTimeMillis()` の直書き禁止
- UI 文言は `strings.xml`。ログに体重・飲酒量を出さない
- テスト: JUnit5（Android Gradle で JUnit5 を使うためのプラグイン or `useJUnitPlatform()`）、kotlinx-coroutines-test、Turbine、Room in-memory は Robolectric か androidTest

## A ↔ B の契約（インターフェース）

B が `domain/` に下記を **最初に** 作る。A はこれに対して ViewModel を書く（それまでは Preview 用のフェイクで UI を作ってよい）。名前・シグネチャを変える場合は報告に書く。

```kotlin
// domain/model
enum class Sex { MALE, FEMALE }
enum class HcSyncStatus { NOT_SYNCED, SYNCED, FAILED }
data class UserProfile(val weightKg: Double, val sex: Sex, val eliminationRatePerHour: Double = 0.015)
data class DrinkPreset(val id: Long, val name: String, val shortName: String, val beverageType: Int,
    val volumeMl: Double, val abvPercent: Double, val icon: String, val colorIndex: Int /*0..5*/,
    val sortOrder: Int, val showInNotification: Boolean)
data class DrinkLog(val id: String, val sessionId: String, val drankAt: Instant, val offset: ZoneOffset,
    val name: String, val beverageType: Int, val volumeMl: Double, val abvPercent: Double,
    val pureAlcoholG: Double, val colorIndex: Int, val hcRecordId: String?)
data class PartySession(val id: String, val startedAt: Instant, val startOffset: ZoneOffset,
    val endedAt: Instant?, val endOffset: ZoneOffset?, val title: String?, val hcSyncStatus: HcSyncStatus)
data class SessionWithLogs(val session: PartySession, val logs: List<DrinkLog>)
sealed interface AppState {             // CLAUDE.md §4 の導出
    data object BeforeParty : AppState
    data class InParty(val data: SessionWithLogs) : AppState
    data class Finished(val data: SessionWithLogs) : AppState
}
enum class HcAvailability { AVAILABLE, NEEDS_UPDATE, UNSUPPORTED }

// domain/calc（純粋関数）
object PureAlcohol { fun grams(volumeMl: Double, abvPercent: Double): Double }
object BacCalculator {
    fun bacAt(logs: List<DrinkLog>, profile: UserProfile, at: Instant): Double      // %
    fun soberAt(logs: List<DrinkLog>, profile: UserProfile, at: Instant): Instant?  // 0 なら null
    fun series(logs: List<DrinkLog>, profile: UserProfile, step: Duration = 5.min): List<Pair<Instant, Double>>
    fun peak(...)
}
object AbstinenceCalculator { /* logicalDate, currentStreak, longestStreak, monthStats, dayStatus(Map<LocalDate, DayStatus>) */ }

// domain/repository（実装は data/repository）
interface PartyRepository { observeAppState(): Flow<AppState>; observeSession(id): Flow<SessionWithLogs?>;
    observeHistory(): Flow<List<SessionWithLogs>>; observeAllLogs(): Flow<List<DrinkLog>>;
    suspend fun startParty(); suspend fun addDrink(presetId: Long): DrinkLog; suspend fun deleteDrink(logId: String);
    suspend fun updateDrink(logId: String, presetId: Long?, drankAt: Instant?); suspend fun endParty(endAt: Instant);
    suspend fun closeSummary(); suspend fun deleteSession(id: String) }
interface PresetRepository { observePresets(): Flow<List<DrinkPreset>>; suspend fun upsert(p: DrinkPreset);
    suspend fun reorder(idsInOrder: List<Long>); suspend fun delete(id: Long) }
interface SettingsRepository { profile: Flow<UserProfile?>; onboardingCompleted: Flow<Boolean>;
    recordStartDate: Flow<LocalDate?>; suspend fun saveProfile(p: UserProfile); suspend fun completeOnboarding() }
interface HealthConnectRepository { suspend fun availability(): HcAvailability; suspend fun hasPermissions(): Boolean;
    val requiredPermissions: Set<String>; suspend fun latestWeight(): Pair<Double, Instant>?;
    fun permissionContract(): ActivityResultContract<Set<String>, Set<String>>; fun enqueueSync(sessionId: String) }
```

UseCase は B が必要に応じて `domain/usecase/` に置く（例: `RecordDrinkUseCase` = 記録 + 通知更新）。**ViewModel / Receiver / Worker は同じ UseCase を通す**（CLAUDE.md §7）。

## フェーズ

| Phase | Codex-A（右上） | Codex-B（右下） |
|---|---|---|
| 1 | Gradle 雛形 → `./gradlew assembleDebug` が通る状態。テーマ（README のトークン: 色・Geist/Noto・形状）、共通部品（ボタン 3 種・ボトムナビ・スナックバー・ドリンクボタン・指標表示）、Navigation 骨格 | Health Connect の未検証事項を調査して `doc/dev/health_connect.md` に記録。domain のモデル・インターフェース・純粋関数（BAC / 断酒 / 純アルコール）とその単体テスト |
| 2 | 全画面の UI（README の 1〜11）+ Preview（ライト / ダーク / フォント 200%）。フェイクデータで | Room（Entity / Dao / DB / exportSchema）、DataStore、Repository 実装、Hilt モジュール、Repository と状態導出のテスト |
| 3 | ViewModel を B の Repository / UseCase に接続。オンボーディング・非対応端末の分岐 | background（通知・DrinkActionReceiver・BootReceiver・HealthSyncWorker・BacUpdateWorker・ForgotToEndWorker）、Health Connect 書き込み / 更新 / 削除 |

各フェーズの終わりに **セルフレビュー** を行い、`doc/dev/report_A.md` / `doc/dev/report_B.md` に追記してから止まる（Claude が確認して次の指示を出す）。

## セルフレビュー（各フェーズ必須）

1. `./gradlew assembleDebug testDebugUnitTest lint` を実行し、結果（成功 / 失敗とその内容）を報告に貼る。他方の作業中でビルドが壊れている場合はその旨を書く
2. `git diff --stat` と新規ファイルを見直し、CLAUDE.md / ハンドオフ README と照合（チェックリスト形式で報告）
3. 確認項目: 文言のハードコード無し / `Instant.now()` 直書き無し / 健康データのログ出力無し / 48dp 以上のタップ領域 / contentDescription / 持ち場外の変更無し
4. 報告の書式:

```
## Phase N（日時）
### やったこと
### ビルド・テスト結果
### セルフレビュー（チェックリスト）
### 未解決・判断したこと
### 相手（A/B）への依頼
```
