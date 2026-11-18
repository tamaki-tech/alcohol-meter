## Phase 1（2026-11-18 13:42 JST）

### やったこと
- Health Connect の権限・記録型・対応判定を公式資料で調査し `health_connect.md` に記録。
- 契約の domain モデル、Repository interface、純粋計算と境界値テストを追加。

### ビルド・テスト結果
- `./gradlew assembleDebug testDebugUnitTest lint`: 失敗（exit 127: `./gradlew` が存在しない）。A の Gradle 雛形がまだ作成中のため、コンパイル・テスト・lint は未検証。

### セルフレビュー（チェックリスト）
- [x] `git diff --stat` を確認（追跡済みファイルの差分なし）。新規ファイルを確認。`git status --short` は `app/` と `doc/dev/` が未追跡として表示されるため、A の並行作業も含む。
- [x] 仕様の式、3時区切り、記録時 offset、契約モデル・Repository を照合。
- [x] UI 文言のハードコードなし（UI 変更なし）。
- [x] `Instant.now()` / `System.currentTimeMillis()` 直書きなし、健康データのログ出力なし。
- [x] 48dp、contentDescription は UI 変更がないため対象外。
- [x] Gradle、UI、その他 A の持ち場を変更していない。

### 未解決・判断したこと
- Jetpack の当該記録型の対応版と feature 判定は未確認。プラットフォーム API の実機検証が必要。
- 契約で省略されていた計算 API は `peak(logs, profile): Double`、断酒は `logicalDate(at, offset)`、`currentStreak/longestStreak(logs, recordStartDate, today)`、`monthStats/dayStatus(logs, recordStartDate, today, month)` とした。月の平均は記録開始日から当日までの対象日数で割る。
- 指定コマンドを実行できなかったため、実装およびテストのコンパイル結果は未確認。

### 相手（A）への依頼
- Gradle 雛形完成後に `./gradlew assembleDebug testDebugUnitTest lint` を実行し、JUnit 5 と domain の依存関係を確認してください。

## Phase 1 修正（2026-11-18）

### やったこと
- 今日の論理日に飲酒がある場合、連続断酒日数を 0 に修正。
- 計算を 3 ファイルに分割し、式・単位の KDoc、明示的 import、モデルのプロパティ整形を追加。
- BAC と断酒の指定境界値を観点別テストに分割して追加。

### ビルド・テスト結果
- 権限昇格して `./gradlew assembleDebug testDebugUnitTest lint` を実行。初回は SDK の場所が未設定で失敗。
- `ANDROID_HOME=$HOME/Library/Android/sdk` を指定して再実行。`:app:compileDebugKotlin` が A 担当の `ui/navigation/Navigation.kt:28` の `onSelect` 引数とラムダの型エラーで失敗。単体テストと lint は実行に到達しておらず未検証。

### セルフレビュー
- [x] 今日飲酒、過去ログ、offset、月集計および指定 BAC 境界のテストを追加。
- [x] UI・Gradle ファイルは変更していない。時刻の直接取得・健康データのログ出力なし。
- [ ] ビルド・テスト・lint 成功は UI のコンパイルエラー解消後に確認が必要。

### 未解決・判断したこと
- 既存の月間平均の定義（記録開始から当日までの日数で除算）を維持。

### 相手（A）への依頼
- `Navigation.kt:28` の `onSelect` 引数のコンパイルエラーを修正後、上記 Gradle コマンドを再実行してください。

## Phase 2（2026-11-18）

### やったこと
- Hilt、KSP、Room、DataStore Preferences、WorkManager / hilt-work、テスト依存を追加し、Hilt Application を Manifest に登録。Room schema を `app/schemas` に出力。
- Room のセッション・飲酒ログ・プリセット、CASCADE、進行中セッションの一意制約、6 件の初期プリセットを追加。Room transaction で多重開始と通知プリセット 3 件上限を検査。
- DataStore のプロフィール・記録開始日・サマリ ID・オンボーディング状態、3 Repository 実装、Clock と IO Dispatcher の Hilt 提供を追加。
- 状態遷移、二重開始、純アルコール量、並べ替え、通知上限、CASCADE の Robolectric テストを追加。

### ビルド・テスト結果
- 権限昇格して `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew assembleDebug testDebugUnitTest lint` を実行。A の作業中の `ui/screens/Screens.kt` の未解決リソースや Compose 呼び出し、`ui/theme/Type.kt` の R 参照エラーにより `compileDebugKotlin` で失敗。テストと lint は到達せず。
- UI と MainActivity を除外し Manifest を置き換えた `/tmp` の一時コピーで `testDebugUnitTest` 成功（Repository 6 件、計算 17 件）。その後の差分はプリセット配列型の明示と取得結果の時刻順ソート。

### セルフレビュー（チェックリスト）
- [x] `git diff --stat` と新規ファイルを確認。追跡済み `.gitignore` の変更は A 側の並行作業であり B は変更していない。
- [x] 計算・状態・初期値を CLAUDE.md とハンドオフに照合。飲料種別の整数は Android 公式 AlcoholConsumptionRecord 定数に対応。
- [x] 文言のハードコードなし（初期プリセットのデータ名を除く）、時刻の直接取得なし、健康データのログ出力なし。
- [x] 48dp / contentDescription は UI を変更していないため対象外。
- [x] A の UI・MainActivity は変更していない。Gradle と Manifest は今回の指示に従い変更。
- [ ] 全体ビルド・lint と実際のアプリ構成でのテスト成功は未確認。

### 未解決・判断したこと
- セッション終了時の Room と DataStore は別ストアのため単一の原子的トランザクションにはできない。DataStore の ID を先に保存し、進行中セッションを常に優先して状態導出する。異常終了時の復旧やサマリの欠落について Phase 3 の結合時に追加検証が必要。
- 進行中 1 件の部分ユニーク index は Room の schema export に表現できないため DB 作成 callback で作成。将来 migration でテーブルを再作成する場合も index を再作成する必要がある。
- 初期プリセットの通知対象は先頭 3 件。記録開始日はオンボーディング完了時の論理日とした。
- HealthConnectRepository は指示どおり interface のまま。

### 相手（A）への依頼
- `MainActivity` に `@AndroidEntryPoint` を付与してください。
- UI のコンパイルエラー解消後、全体の `assembleDebug testDebugUnitTest lint` を再実行してください。

## Phase 3 途中経過（2026-11-18）

### やったこと
- sdkmanager で `platforms;android-37.0` を導入。36 extension の提供は ext19 まで。現行 AGP 8.9.1 で compileSdk 37 を試すと `platforms;android-37` が見つからず失敗したため、リポジトリの設定は 36 に戻した。
- 公式対応表では API 37.0 の最低 AGP は 9.1.1、Gradle は 9.3.1。AGP 9.1.1 / Gradle 9.3.1 / Kotlin 2.2.10 / KSP 2.2.10-2.0.2 を一時コピーで試し、drawable 内のライセンスファイルを一時的に除けば compileDebugKotlin 成功。旧 Kotlin / DSL の互換フラグは非推奨。
- HC マッピング用の純粋な中間データと 6 時間後の判定関数、および単体テストを追加。まだ実際の HC API・Worker には接続していない。

### ビルド・テスト結果
- 権限昇格で `assembleDebug testDebugUnitTest lint` を実行したが、`app/src/main/res/drawable/LUCIDE_LICENSE.txt` がリソース拡張子として不正となり mergeDebugResources で停止。lint とテストは到達せず。
- UI を除いた一時コピーで `testDebugUnitTest` 成功（既存 23 件、マッピング 1 件、通知時刻 2 件）。

### セルフレビュー
- [x] `git diff --stat` と新規ファイルを確認。UI と A のリソースは変更していない。
- [x] 追加処理に時刻の直接取得、健康情報のログ、UI 文言なし。タップ領域と contentDescription は対象外。
- [ ] Phase 3 は未完了。HealthConnectRepositoryImpl、UseCase 群と通知・Receiver・Worker、Manifest 権限、同期と削除、残りのテストは未実装。

### 未解決・判断したこと
- A のリソースを移動せずに AGP を変更するとビルドできないため、AGP 更新は一時コピーの検証に留めた。SDK 調査: https://developer.android.com/build/releases/about-agp 、 https://developer.android.com/build/releases/agp-9-1-0-release-notes
- 終了後の削除は同期前に ID が失われないよう、永続的な削除待ちキューと再試行の設計が必要。未実装の同期機能を完成済みとして扱わないこと。

### 相手（A）への依頼
- `res/drawable/LUCIDE_LICENSE.txt` を Android リソース外の適切なライセンス配置へ移動してください。

## Phase 3 続行中（2026-11-18）

### やったこと
- 承認に従い AGP 9.1.1、Gradle Wrapper 9.3.1、Kotlin 2.2.10、KSP 2.3.10、Hilt 2.60.1、compileSdk / targetSdk 37 に更新。AGP 9 標準 Kotlin を使用し互換フラグなし。
- Room に `hc_pending_deletion` を追加し、削除と同一 transaction で client ID を保存。同期中の編集を検出するためログに `hcVersion` を追加し、version 1 の schema を更新。
- HC プラットフォーム API、権限・体重読み取り、書き込み・更新・削除、削除キューを先に処理する同期 UseCase / Worker と再試行を実装。通知、Receiver、WorkManager、UseCase、Manifest の権限と理由 Activity を追加中。
- 削除キュー、同期成功・失敗・同期中削除、UseCase の副作用、6 時間境界などのテストを追加。

### ビルド・テスト結果
- リポジトリ本体の `assembleDebug testDebugUnitTest lint` は `compileDebugKotlin` で失敗。通知・権限説明用の未追加文字列、および A が並行実装中のカレンダー文言・履歴画面の引数に未解決エラーがある。テスト・lint は到達せず。
- UI を除き通知文言を仮置きした一時コピーでは 39 件のテストと lint が成功。実リポジトリの全体成功や実機 HC 動作を意味しない。

### セルフレビュー（途中）
- [x] `git diff --stat`、新規ファイル、直接時刻取得・健康データ出力を確認。追跡済み `.gitignore` の変更は A 側。
- [x] A の UI は変更していない。通知アイコンのみ許可された drawable として追加。
- [ ] 全体ビルド・テスト・lint と実機の Health Connect 動作は未検証。Phase 3 は完了していない。
- [ ] 通知文言・権限理由文言の res/values への追加は A の持ち場のため確認待ち。理由 Activity の文言と公開用プライバシーポリシーの整合も確認が必要。

### 未解決・判断したこと
- 終了済みログは HC 挿入後にローカル保存前にプロセスが停止する場合も考慮し、HC ID の有無に関わらず削除キューへ積む。
- 同期予約前にプロセスが停止した場合の保留キュー再発見、実機での権限・記録 API 検証、ViewModel からの UseCase 接続は残る。
- A の UI 変更が並行中のため、Gradle 更新が原因と特定できない UI のエラーには触れていない。

### 相手（A）への依頼
- `res/values` の通知・権限理由文字列を B が別ファイルで追加してよいか確認中。A が追加する場合は通知 channel 名、飲み会タイトル・杯数と BAC、6 時間通知・終了時刻・続ける、HC 権限理由の文言が必要。
- ViewModel を domain/usecase に接続し、アプリ再表示時に通知を復元する呼び出しを追加してください。

## Phase 3 文言・起動時復旧（2026-11-18）

### やったこと
- 許可に従い `res/values/strings_background.xml` に `notif_` / `hc_` の文言を追加。既存 values の string 名と重複がないことを確認。
- `ReconcileBackgroundState` を追加。Application 起動時と BootReceiver から同じ UseCase を呼び、終了済み・未同期 / 失敗セッションと削除待ちを再予約する。復旧予約はセッションごとに `enqueueUniqueWork(..., KEEP)`。進行中なら通知と BAC 更新・お開き忘れ Work を再設定する。
- 同期処理にプロセス内 mutex を設け、通常予約と復旧予約が同時に実行されても削除と書き込みが並列にならないようにした。
- 復旧 UseCase の対象選択・飲み会中の復元・対象なし、Room の終了済み未同期フィルターをテスト。

### ビルド・テスト結果
- 実リポジトリで権限昇格して `assembleDebug testDebugUnitTest lint` を実行。`compileDebugKotlin` は A の作業中の `ui/screens/calendar/CalendarScreen.kt`、`history/HistoryDetailScreen.kt`、`settings/PresetEditorScreen.kt` / `SettingsScreen.kt` のエラーで停止。出力上 B 担当コードのエラーはなし。全体のテスト・lint は未到達。
- UI を除いた一時コピーで `testDebugUnitTest lint` 成功。テスト 43 件、lint エラー 0。仮 UI のため全画面を含む検証ではない。

### セルフレビュー（チェックリスト）
- [x] `git diff --stat` と新規ファイルを確認。追跡済み `.gitignore` の変更は A の並行作業。
- [x] 背景文言は許可された別ファイルに置き、名前の重複なし。
- [x] `Instant.now()` / `System.currentTimeMillis()` 直書きなし。健康データをログに出していない。
- [x] 48dp / contentDescription は B の画面変更なしのため対象外。権限説明 Activity はテキスト表示のみ。
- [x] UI・A の strings.xml は変更していない。Gradle / Manifest / 通知 drawable / 専用文言は指示に基づく変更。
- [ ] 全画面を含むビルド・lint および実機の HC・通知は未検証。

### 未解決・判断したこと
- 未リリースのため DB version 1 の schema を更新。既に旧 version 1 DB を作った開発端末ではアプリデータの消去が必要。
- `DrinkLog` に同期バージョン（既定値 1）を追加。同期途中に編集・削除があった場合は同期状態を確定せず再試行する。
- 既存の通常同期と復旧同期は別の一意名を使うため両方が存在する場合があるが、同期本体は mutex で直列化する。
- HC 権限説明の Activity はデータ利用の説明を表示する。公開時には Play Console に登録するプライバシーポリシーと一致する内容・導線が必要。

### 実機で確認すべき項目
- Android 14〜16 の U Extensions 20 以下 / 21 以上と Android 17 で対応判定、非対応画面、クラス読み込みを確認。
- 飲酒書き込み・体重読み取りの権限許可と拒否、設定から再許可、理由画面と最新体重の取得を確認。
- 1 杯の時刻・offset・種類・量・度数・メモ・手動入力・clientRecordId、再送時の重複防止、編集・削除反映を Health Connect で確認。
- 同期失敗時の FAILED / 再試行、同期中の編集・削除、削除キュー保存後・Work 予約前の強制終了からの復旧を確認。
- 通知権限、スワイプ消去、アプリ再起動・端末再起動での再表示、3 アクション、BAC の 15 分更新、6 時間通知と両アクションを確認。
- オンボーディングと A の ViewModel が UseCase を呼び、アプリ復帰時の通知復元を含む全画面フローを確認。

### 相手（A）への依頼
- ViewModel を `domain/usecase` に接続し、全体ビルドの UI エラー修正後に `assembleDebug testDebugUnitTest lint` を再実行してください。

## ViewModel 接続（2026-11-18）

### やったこと
- Hilt ViewModel をホーム、サマリ、履歴一覧・詳細、カレンダー、設定・プリセット編集、オンボーディングに追加。Repository 操作は UseCase 経由とし、UI 用 StateFlow とイベント関数を公開。
- ホームの 1 秒経過表示、記録時・1 分ごとの BAC、直前の記録のみ 5 秒取り消し、お開き時刻の選択を接続。サマリと履歴に BAC series / peak / しらふ時刻、同期再送、編集・削除を接続。
- カレンダーの月送り・日付から履歴への遷移、プリセット並べ替え・通知上限、体重・性別・分解速度の編集、HC 権限の再要求を接続。
- MainActivity を Hilt 対応し、対応チェック・オンボーディング・4 タブへの分岐と、アプリ再表示時のバックグラウンド復旧を接続。お開き忘れ通知からは最後の 1 杯を初期選択。
- 数値・時刻・日付の書式関数とテスト、ホーム・カレンダー・オンボーディング・プリセット・履歴編集の ViewModel テストを追加。日本語の表示用文言は `strings_viewmodel.xml` に追加。
- 実際のログ ID と編集値を返すため履歴詳細、設定の操作コールバック、オンボーディング性別未選択、HC 飲料種別値に限り既存画面を変更。

### ビルド・テスト結果
- 権限昇格で `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew assembleDebug testDebugUnitTest lint` を実行し成功。単体テスト 54 件成功、失敗 0、lint エラー 0。

### セルフレビュー（チェックリスト）
- [x] `git diff --stat` と新規ファイルを確認。追跡済み `.gitignore` の変更は A の作業。Gradle に Hilt Navigation Compose を追加。
- [x] ViewModel / navigation / MainActivity に `Instant.now()`、`System.currentTimeMillis()`、健康データのログ出力なし。表示用日本語文字列はリソース化し、既存の string 名との重複なし。
- [x] 追加した設定行のタップ領域は 52dp。既存画面のアクセシビリティ・レイアウトは A の実装を使用。
- [x] 取り消し期限と直前の記録、オンボーディングの体重・性別必須、月送り、通知上限、履歴編集後の同期予約をテスト。
- [ ] 実機・エミュレータでの権限 UI、通知からの復帰、全画面操作とフォント拡大は未確認。

### 未解決・判断したこと
- 保存済みログはプリセット ID を持たないため、ホームの種類別杯数・直前プリセットは保存名との照合で導出している。プリセット名の変更後は過去の杯を元のプリセットに結び付けられない。
- サマリの BAC series は対象セッション以前の残存アルコールも計算に含め、対象セッションの最初の杯以降を表示する。前のセッションからの持ち越しは単体テストで確認。
- UI 上の開始・終了・杯の表示時刻は現在の端末タイムゾーンで表示する。記録時 offset は論理日・保存・HC 連携に使用する。
- 実機で HC の権限拒否・再許可、途中終了と再起動、通知のアクション、履歴編集・削除と同期、オンボーディング、設定入力を確認してください。

### 相手（A）への依頼
- 追加済みのオンボーディング / 非対応画面と設定・履歴画面の最小変更を含め、ライト / ダーク / フォント拡大時の見た目を確認してください。

## 実機確認後の修正（2026-11-18）

### やったこと
- `enableEdgeToEdge()` とルートの `WindowInsets.safeDrawing` を追加。4 タブ・オンボーディング・非対応画面に同じ余白を適用し、ボトムナビをシステム領域の上に配置。システムバーの明暗は edge-to-edge の自動判定とアプリのシステムテーマ追従を使用。
- 経過時間は H:MM:SS のまま、お開き確認とサマリ・履歴詳細の所要時間を「2 時間 42 分」「42 分」形式に変更。サマリタイトルに「の飲み会」を追加。
- 履歴がない場合の recentParty を null にし、直近の飲み会欄を非表示にした。
- `drink_log.preset_id`（nullable、FK なし）を追加し、記録時と履歴編集時に保存。プリセット名を変更しても ID で杯数と直前のプリセットを追跡する。ID のない旧形式ログは名前・種類・量・度数の一致で照合。
- DB version 1 の Room schema を再出力。表示書式、タイトル、履歴なし、名前変更後の一致、ID の保存・編集をテスト。

### ビルド・テスト結果
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew assembleDebug testDebugUnitTest lint`：成功。単体テスト 60 件、失敗 0、lint エラー 0。
- 初回実行は A の並行作業中の `UndoSnackbar` とカレンダー画面のコンパイルエラーで失敗。画面側の更新後に上記コマンドを再実行して成功。

### セルフレビュー（チェックリスト）
- [x] `git diff --stat` と新規ファイルを確認。追跡済み `.gitignore` は A 側の変更。DB / schema はユーザーが許可した preset_id 方式による変更。
- [x] 時刻の直接取得・健康データのログ出力なし。既存の 48dp タップ領域と contentDescription を維持。
- [x] 既存の ViewModel 接続と HC 同期テストを含む全体テストが成功。
- [ ] インセットとシステムバーの明暗、横向き・ジェスチャーナビ・フォント拡大時の実機表示は未確認。
- [ ] ユーザー指定の純粋な `ui/format` 関数として「時間」「分」「の飲み会」を文字列に含めた。CLAUDE.md の UI 文言を strings.xml に置く原則との整合は要確認。

### 未解決・判断したこと
- 未リリースのため version 1 の schema を更新。旧 schema の DB が残る開発端末ではアプリデータの消去が必要。
- `preset_id` のない既存ログは後から正確な ID を復元できない。プリセット名を変更済みの場合は旧ログの杯数を元のプリセットへ関連付けられない。

### 相手（A）への依頼
- 画面側でインセットを重複適用しないことと、直近の飲み会がない場合の表示を実機で確認してください。

## 未連携表示・書式の修正（2026-11-18）

### やったこと
- 飲酒書き込み権限がない場合、同期処理を成功として終了し、セッションを `NOT_SYNCED` に戻す。削除待ちは保持し、再試行は行わない。処理途中に権限を失った場合も同様に扱う。
- 書き込み権限の状態を共有し、権限がない間は保存済み状態が FAILED でもサマリ・履歴詳細を「未連携」、履歴タグを「未連携」にする。
- 設定からの権限変更後に状態を更新し、書き込み権限を得た場合は `ReconcileBackgroundState` で未送信分と削除待ちを再予約する。
- 所要時間は純粋関数で時間・分に分解し、ViewModel で文字列リソースから組み立てる。サマリタイトルと月見出しの日本語もリソースへ移した。

### ビルド・テスト結果
- 権限昇格で `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew assembleDebug testDebugUnitTest lint` を実行し成功。単体テスト 62 件、失敗 0、lint エラー 0。
- 権限なしで成功終了・NOT_SYNCED・削除待ち保持、後から許可して同期、未連携時の詳細表示、権限付与後の再予約、所要時間の分解とリソース表示をテスト。

### セルフレビュー（チェックリスト）
- [x] `git diff --stat` と新規ファイルを確認。追跡済み `.gitignore` は A 側の並行変更。
- [x] `ui/format` に日本語の表示文言なし。日時・数値処理は純粋関数、時間・分・サマリタイトル・月見出しはリソースから組み立てる。
- [x] 時刻の直接取得・健康データのログ出力なし。UI のタップ領域と contentDescription は変更していない。
- [ ] 権限拒否・後日許可の実機フローは未確認。

### 未解決・判断したこと
- 同期できるかは飲酒の書き込み権限で判定する。体重読み取り権限のみが拒否された場合も、書き込み権限があれば飲酒記録を同期する。

### 相手（A）への依頼
- なし。Claude による実機確認時、未連携でお開き後に設定から許可し、送信済みに変わる流れを確認してください。
