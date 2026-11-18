## Phase 1（2026-11-18）
### やったこと
- Gradle Wrapper 8.11.1 と AGP 8.9.1、Kotlin 2.1.10、Compose Compiler plugin の単一 app モジュール構成を追加（compileSdk/targetSdk 36、minSdk 34、JDK 17）。
- AndroidX と JUnit 5 を設定。既存 domain の JUnit 5 テストも実行対象となる依存を設定。
- デザイントークンに沿った Material 3 のダーク/ライトテーマ、プリセット6色、形状・タイポグラフィ、Primary/Secondary/Ghost、ボトムナビ、Undo Snackbar、DrinkButton、Metric を追加。
- Home / History / Calendar / Settings のプレースホルダーを Compose Navigation で接続。
- Android Manifest と文字列・プラットフォームテーマを追加。既存の domain と作業中差分は変更していない。

### ビルド・テスト結果
- `ANDROID_HOME=/Users/nmatsumoto/Library/Android/sdk ./gradlew assembleDebug testDebugUnitTest lint` — **BUILD SUCCESSFUL**（Gradle 8.11.1）。assembleDebug、JUnit 5 unit tests、lint が完了。
- Compose compile の `outlinedButtonBorder` deprecated 警告と Gradle 9 非互換機能の警告あり。lint は成功。

### セルフレビュー（チェックリスト）
- [x] UI 固有のユーザー向け文字列は `strings.xml` に配置（共通部品の引数で受けるラベルは呼び出し側でリソース化すること）。
- [x] UI/MainActivity に `Instant.now()` / `System.currentTimeMillis()` 直書きなし。
- [x] 健康データのログ出力なし。
- [x] ボタン類とナビ項目は最低 48dp のタップ領域を確保。DrinkButton は 88dp。
- [ ] アイコン相当のボトムナビ記号に個別 contentDescription 未設定。Phase 2 の Lucide アイコン実装時に追加が必要。
- [x] 変更範囲は Gradle 構成、app UI/manifest/res、report_A.md のみ。domain/data/background/di と既存 Claude/ユーザー差分には触れていない。
- [x] 変更ファイル確認: Gradle ルート構成、`app/src/main` 配下の新規ファイル。`git diff --stat` は未追跡ファイルを表示しないため、`git status --short` と `find app/src/main -type f` でも確認。

### 未解決・判断したこと
- Geist / Noto Sans JP のライセンス済みフォントファイルがリポジトリに無いため、現時点ではシステム Sans fallback を設定。フォント配布資産が揃った Phase 2 で同梱・適用する。
- 共通ボトムナビの暫定アイコンは文字記号。Lucide VectorDrawable を追加する段階で置換し contentDescription を付ける。
- 本文/画面固有の高 fidelity UI は Phase 2 対象で、現段階は Navigation の画面骨格。

### 相手（A/B）への依頼
- Codex-B: app/build.gradle.kts と libs.versions.toml は共有管理です。依存追加時は既存 alias を保ち、変更内容を報告してください。Gradle Wrapper 8.11.1 / AGP 8.9.1 / compileSdk 36 でビルド確認済みです。

## Phase 2（2026-11-18）
### やったこと
- `ui/screens/UiStates.kt` に画面ごとの immutable UiState を定義し、画面 Composable は state と callback を受け取る形で実装。Preview 専用フェイク値を追加。
- 画面 1/1'、2（1b の記録優先）、3 終了ダイアログ、4 サマリと Canvas BAC 線グラフ、5 履歴、6 詳細と編集シート、7 カレンダー、8 設定、9 プリセット編集、10 3つの入力ステップ、11 非対応状態を追加。
- Light/Dark/200% fontScale の Preview 群を実装。Navigation から仮の画面状態を表示。
- 公式 Google Fonts の Geist Regular/Semibold を res/font に同梱（OFL）。Noto Sans JP は端末フォントに委任。
- 飲み会中の記録ボタンに長押し相当の触覚、1.25 倍 scale animation、Undo snackbar と 5 秒の残り時間バーを追加。
- Gradle ファイルは編集していない。Claude が追加した Hilt/KSP/JUnit5 設定を維持。

### ビルド・テスト結果
- `ANDROID_HOME=/Users/nmatsumoto/Library/Android/sdk ./gradlew assembleDebug testDebugUnitTest lint` — **BUILD SUCCESSFUL**。
- 17 unit tests 実行済み。lint 成功。Kotlin compile の `outlinedButtonBorder` deprecated 警告あり。

### セルフレビュー（チェックリスト）
- [ ] 画面の一部文言は Kotlin 内に残っている。画面テキスト全体の strings.xml 抽出は未完。
- [x] `Instant.now()` / `System.currentTimeMillis()` の直書きなし。
- [x] 健康データのログ出力なし。
- [x] Primary/Secondary/Ghost、DrinkButton、カレンダー日セル、行、ナビは概ね48dp以上（DrinkButton 88dp、CTA 56dp/112dp）。
- [ ] アイコンは文字記号の仮表現が残り、contentDescription 未対応。Lucide SVG / VectorDrawable 資産の実装は未完。
- [x] 変更範囲は ui/、res/、report_A.md に限定。Gradle、Manifest、domain/data/background/di は変更なし。Claude の .gitignore / local.properties 変更も維持。
- [x] `git status --short` と A の画面資産を見直し。`git diff --stat` は新規未追跡ファイルの統計を含まないため、新規ファイル一覧も確認。

### 未解決・判断したこと
- Phase 2 の UI は stateless の状態/callback 構成だが、いくつか callback はまだ仮実装（編集種別選択や入力、削除確認など）。ViewModel 接続と操作仕上げは Phase 3 で必要。
- デザイン準拠を優先して画面の日本語テキストを直書きした箇所が残る。文字列リソース化が必要。
- Lucide VectorDrawable と酒種ごとの正式アイコンは未作成。仮記号には個別 contentDescription も未設定。
- スナックバー時間バーは5秒で縮退する。5秒後の実データ確定/Undo期限処理は ViewModel と接続時に実装する。
- UI は build/lint 済みだが、Compose Preview のレンダリング画像での目視 QA はこの環境では未実施。

### 相手（A/B）への依頼
- Codex-B への Gradle 依存依頼なし。Phase 3 で画面 state を ViewModel/Repository と接続し、仮 callback を実動作にしてください。

## Phase 2 やり直し ステップ 1・2（2026-11-18）
### やったこと
- 共通部品を1ファイル1部品に分け、AppButton / DrinkButton / MetricStrip / UndoSnackbar / AppBottomNav / DrinkColorDot を実装。ホーム中画面・Navigation から呼び出し。
- `ui/screens/home/` に `HomeUiState.kt`、`HomeBeforeScreen.kt`、`HomeInPartyScreen.kt` を追加。従来 Screens.kt の開始前/飲み会中画面は Navigation 呼び出しから外した。
- README 対応表:
  - AppButton → 「ボタン」: Primary/Secondary/Ghost、48/56/112dp、高さ別角丸、押下色、リップル無し、任意 drawable と説明を実装。
  - DrinkButton → 「飲み会中 2-7」: 88dp、divider 1dp、角丸10、内側8/10dp、24dpアイコンとプリセット色、×N 17/600、shortName 15/600、帯3dp、押下tint、記録contentDescription。
  - MetricStrip → 「飲み会中 2-2」: 1.25:1:1.15 比率、値34/600、単位15、ラベル11、2・3列目仕切りと左12dp、杯数値を左下基準で1.25倍90ms + 1倍90ms。
  - UndoSnackbar → 「スナックバー」: 左右12dpで配置する呼び出し、反転色、取り消し16/600/下線/48dp、3dp残りバーを5秒で縮小。
  - AppBottomNav → 「ボトムナビ」: 上辺divider、64dp、4等分、Lucide 22dp、ラベル11、active primary / inactive muted、インジケータなし、リップルなし。
  - DrinkColorDot → 「最近の3杯」: 円10dp。ホームの各行は色丸 + 等幅時刻44dp + 名前14sp、高さ26dp。
  - HomeBeforeScreen → 画面1/1': 左右16dp、今日12sp、日付24sp、休肝カードpadding 18/20dp・角丸12dp、数字96sp、前回BAC 64sp、乾杯 CTA 112dp/角丸14dp。
  - HomeInPartyScreen → 画面2: 上部56dp、左padding20dp、8dp状態丸、見出し19sp、時刻13sp、お開き48dp。最近3杯とUndo、固定「もう1杯」CTA、3列8dp間隔グリッド。プリセット数に応じグリッドがスクロール。

### ビルド・テスト結果
- `ANDROID_HOME=/Users/nmatsumoto/Library/Android/sdk ./gradlew assembleDebug testDebugUnitTest lint` を実行。
- **失敗**: Kotlin コンパイル段階で Codex-B の `PartyNotificationController.kt` / `PermissionsRationaleActivity.kt` が参照する `notification_channel_party`, `notification_channel_reminder`, `notification_party_title`, `notification_party_body`, `notification_reminder_body`, `notification_end_at`, `notification_continue`, `health_permissions_rationale` が見つからない。A の UI コンパイルエラーはこの実行出力に出ていない。テスト/lint は Kotlin compilation failure で未到達。

### セルフレビュー（チェックリスト）
- [x] 対象のホーム画面は専用 UiState を受け取る stateless Composable。
- [x] UI/MainActivity に `Instant.now()` / `System.currentTimeMillis()` 直書きなし、健康情報ログなし。
- [x] ホーム操作領域は48dp以上。取消、ドリンク記録などに明示的説明を設定。
- [x] Gradle ファイルと domain/data/background/di は変更なし。
- [ ] 残る旧画面（サマリ/履歴/設定等）は旧 Screens.kt 内に存在し旧部品名の呼び出しもある。該当旧画面の削除・分割は次ステップ対象として残る。

### 未解決・判断したこと
- Full build は Codex-B 側の8文字列リソース不足で停止。B の作業が揃った後に統合ビルド再実行が必要。
- Phase 2 やり直しの他画面（README 3〜11）改修および旧 Screens.kt 全体のファイル分割は今回のステップ対象外として未実施。

### 相手（A/B）への依頼
- Codex-B: 現在の Kotlin compilation failure の通知/権限文字列リソースを追加してください。追加後、A UI を含めたビルド再確認をお願いします。

## Phase 2 やり直し ステップ 3・4（2026-11-18）
### やったこと
- `ui/screens/summary/` に終了確認、サマリ、共有 SessionSummaryContent と専用 UiState を追加。
- `ui/screens/history/` に履歴一覧、詳細、編集ボトムシート、削除確認ダイアログ、専用 UiState を追加。
- Navigation から新規サマリ/履歴画面へ接続し、Screens.kt から旧終了確認・サマリ・履歴一覧/詳細/編集シートを削除。
- README 対応表:
  - EndPartyConfirmation → README 3: 下寄せ・左右16/下24、scrim 72%、終了時刻2択、state 引数で初期選択、選択側 primary、まだ飲む/お開き 56dp。
  - SessionSummaryScreen + SessionSummaryContent → README 4: 2×2 指標と divider、種類タグ、BAC Canvas（primary 1.75dp 線・16% 下塗り・杯マーカー半径3dp・2時間ごとのラベル）、最大BAC、しらふ目安/注意、飲酒行48dp、同期3状態、閉じる56dp。
  - HistoryListScreen → README 5: 月グループ、日付/時刻/概要、68dp行、送信済み/失敗/未連携タグ。失敗タグから retry callback。
  - HistoryDetailScreen → README 6: 56dp 上部バー、戻る/日付/メニュー、セッション削除確認、SessionSummaryContent をサマリと共有、杯行52dp。
  - EditDrinkBottomSheet → README 6: 下端配置・上角20dp・36×4dpハンドル、3列プリセットチップ、時刻欄、HC更新注記、範囲外エラー、削除/キャンセル/保存。

### ビルド・テスト結果
- `ANDROID_HOME=/Users/nmatsumoto/Library/Android/sdk ./gradlew :app:compileDebugKotlin` 実行。A の追加画面に起因する Kotlin エラーは解消済み。
- 統合コンパイルは Codex-B 側の notification/health rationale strings 8件（`notification_channel_party`, `notification_channel_reminder`, `notification_party_title`, `notification_party_body`, `notification_reminder_body`, `notification_end_at`, `notification_continue`, `health_permissions_rationale`）の未定義で失敗。build/test/lint フルセットはこのため未到達。

### セルフレビュー（チェックリスト）
- [x] 新規画面は専用 state を受け取り、イベントは callback。ViewModel 接続なし。
- [x] A UI に `Instant.now()` / `System.currentTimeMillis()` 直書きや健康情報ログなし。
- [x] 操作部品は48dp以上。詳細のタップ行52dp、履歴行68dp。
- [x] 変更は A の ui/screens、ui/navigation、res/values/strings.xml、報告書。Gradle/Manifest/domain/data/background/di は編集していない。
- [ ] 編集中の時刻欄は UI 上の選択欄まで。Android TimePicker 呼び出しと開始〜終了範囲での保存 callback 抑止は未接続（次フェーズ UI state/callback 実装で必要）。
- [x] サマリと詳細で SessionSummaryContent を共有。

### 未解決・判断したこと
- TimePicker は ViewModel 接続前提の callback 設計とし、現段階では欄から日時選択ダイアログをまだ開かない。
- HistoryDetailScreen の編集シート表示は state.edit が非 null のとき。呼び出し側が対象ログの編集状態を供給する。
- Full Gradle review は B の strings 未定義により実行未完了。B 側統合後に再実行が必要。

### 相手（A/B）への依頼
- Codex-B: report_A の前述8件の strings 未定義を追加してください。完了後、フルビルドとテストを再実行します。

## Phase 2 やり直し ステップ 5・6（2026-11-18）
### やったこと
- 編集シートの時刻欄から Material3 TimePicker（24時間表示）を開き、選択時刻を UI state に反映。飲み会開始〜終了外なら muted 理由を表示して保存ボタンを無効化。
- CalendarScreen / SettingsScreen / PresetEditorScreen を各専用 package に実装し、Screens.kt から旧 Calendar / Settings / PresetEditor 実装・Preview を削除。
- Navigation の calendar/settings route をフェイク UiState で新 Composable に接続。ViewModel は未接続。
- README 対応表:
  - CalendarScreen → README 7: 年月見出し28sp、48dp前後移動ボタン、未来月進行抑止、7列/4dp間隔、42dp/角丸8日マス、14sp数字/5dp丸、domain DayStatus 4状態、凡例AM3:00、連続/最長/今月/飲酒日/平均指標、日付 callback。
  - SettingsScreen → README 8: 52dpプリセット行、長押し drag gesture 並べ替え、grip/bell drawable、追加ボタン、推定 BAC 設定行52dp、AM3:00固定表示。
  - PresetEditorScreen → README 9: 上部バー、160×92 preview、48dpフォーカス枠付き入力、数値キーボード/単位、種別一覧、6色44dp、通知制限理由、純アルコール量の自動算出。
  - EditDrinkBottomSheet → README 6 残件: TimePicker 起動、24時間、範囲内判定、範囲外保存無効、12sp muted 理由。

### ビルド・テスト結果
- `ANDROID_HOME=/Users/nmatsumoto/Library/Android/sdk ./gradlew :app:compileDebugKotlin` — **BUILD SUCCESSFUL**。A の ui/ 配下 Kotlin コンパイルエラー 0 件。
- フル assemble/test/lint は未実行。直前までの全体コンパイルで Codex-B の notification / health rationale strings 8件未定義が確認されていたため、A 範囲のみの compileDebugKotlin で確認。

### セルフレビュー（チェックリスト）
- [x] UI 文言は strings.xml 参照（weekday は固定リソース）。
- [x] UI に現在時刻 API 直書き、健康データログなし。
- [x] カレンダー操作は48dp、設定行52dp、編集入力48dp、スウォッチ44dp。
- [x] Navigation はフェイク state を渡し、画面は UiState/callback 方式。
- [x] 旧カレンダー/設定/プリセット編集の画面定義とPreviewをScreens.ktから削除。
- [x] Gradle、Manifest、domain/data/background/di は変更なし。

### 未解決・判断したこと
- 月グリッドは 42dp 日セルと4dp行間を含む固定表示高さで構成。
- プリセット長押しのドラッグは LazyColumn ではなく縦スクロール Column の各行 pointer gesture で index 差分を callback に通知。Repository の永続化接続は Phase 3。
- TimePicker の選択結果は一時的な画面 state に保存。呼び出し側の ViewModel への永続反映は Phase 3。

### 相手（A/B）への依頼
- Codex-B: notification / rationale の文字列参照を解消したら統合 assemble/test/lint を実行してください。A の UI compilation は成功確認済みです。

## Phase 2 やり直し ステップ 7（2026-11-18）
### やったこと
- `ui/screens/onboarding/` に OnboardingUiState / OnboardingScreen を追加。
- `ui/screens/unsupported/` に UnsupportedUiState / UnsupportedScreen を追加。
- UiStates.kt と Screens.kt を削除し、既に専用 package に移行した画面実装・state の旧置き場を整理。
- Navigation.kt / MainActivity.kt は変更していない。
- README 対応表:
  - OnboardingScreen → README 10: 4分割進捗バー高さ3dp/角丸999/間隔4、見出し32/600、補足14sp/70%、下部CTA56dp。
  - 体重・性別段階: 64sp体重値とkg、primary下線2dp、decimal keyboard、Health Connect 値の日付注記、性別2択56dp。
  - Health Connect段階: 2行の権限説明、許可CTA、連携を飛ばすGhost。
  - 通知段階: 常駐通知見本カード、通知許可CTA、あとでGhost。
  - UnsupportedScreen → README 11: 中央寄せ＋下80dp移動、警告drawable枠72dp、見出し/説明、divider後にAndroid版注記、操作ボタンなし。
  - 両画面に Light / Dark / fontScale 2.0 Preview。

### ビルド・テスト結果
- `ANDROID_HOME=/Users/nmatsumoto/Library/Android/sdk ./gradlew assembleDebug testDebugUnitTest lint` — **BUILD SUCCESSFUL**。
- assembleDebug 成功、Unit test task 成功、lint 0 errors。

### セルフレビュー（チェックリスト）
- [x] 新画面は専用 UiState を受け取り、イベント callback 経由。
- [x] 画面表示文言は strings.xml から取得。
- [x] 時刻 API の直呼び出しなし、健康データのログ出力なし。
- [x] 性別選択・CTA・スキップは48dp以上、主要CTAは56dp。
- [x] アイコン drawable を使い、絵文字・仮記号なし。警告アイコンは装飾として contentDescription null。
- [x] 変更範囲は ui/screens、res/values/strings.xml、report_A.md。Navigation/MainActivity/Gradle/domain/data/background/di は変更なし。

### 未解決・判断したこと
- Onboarding step 1 は画面を表示しない対応判定段階として扱い、表示画面は weight/sex (step 2)、Health Connect (step 3)、notification (step 4) の state.step 値に対応。Composable 内では1/2/3の表示番号を 1/2/3 に正規化したフェイク Preview を使う。

### 相手（A/B）への依頼
- なし。

## 実機レビュー修正（2026-11-18）
### やったこと
- UndoSnackbar を HomeInPartyScreen の親 Box 内 BottomCenter に配置し、コンテンツ高さ54dp（操作行48dp + 残り時間バー3dp）に修正。画面全体を覆わず、グリッド操作を妨げない。
- MetricStrip の数字に等幅数字を適用し、値長に応じてフォントを段階縮小。最大「10:42:18」も1行で収める設定（maxLines=1 / softWrap=false）。
- もう1杯ボタンを左揃えの縦レイアウトへ変更。13sp/500ラベルの下に30dpアイコンと30sp/600飲料名を横並び。
- カレンダー曜日を日〜土の個別 strings に修正。TODAY 枠の1dp primaryを維持し、飲酒日は onSurface の5dp丸に変更。
- 通知見本の `%` 二重表示を修正し、オンボーディングルートに imePadding を追加。未選択の性別ボタンは Secondary（divider枠）を表示。

### ビルド・テスト結果
- `ANDROID_HOME=/Users/nmatsumoto/Library/Android/sdk ./gradlew assembleDebug testDebugUnitTest lint` — **BUILD SUCCESSFUL**。
- assembleDebug / testDebugUnitTest / lint 完了。lint errors 0。Unit test reports に全テスト成功を確認（43件）。

### セルフレビュー（チェックリスト）
- [x] Navigation.kt / MainActivity.kt / ViewModel は変更なし。
- [x] UndoSnackbar は画面高を占有せず、タップ可能なドリンクグリッドの上に重ねて表示。
- [x] 等幅数字と時間文字列長に基づく段階サイズ縮小を適用。
- [x] strings.xml の曜日と通知見本を修正。
- [x] UI コンパイル・assemble・unit tests・lint が成功。

### 未解決・判断したこと
- なし。

### 相手（A/B）への依頼
- なし。

## 実機レビュー追加修正（2026-11-18）
### やったこと
- AppButton の drawable icon に foreground tint を適用し、disabled 時の icon alpha を指定可能にした。カレンダーの翌月ボタンは進めない月で無効化し、アイコンを 35% 表示。
- CalendarUiState の `today` と日付セルの状態を独立して扱い、DayStatus が DRINKING の今日にも 1dp primary 枠が表示されるよう修正。
- カレンダー凡例の「飲んだ日」マークをプリセット色から onSurface に変更。
- UI の Image 使用箇所を確認。AppButton 共通アイコンのほか、ホームの「もう 1 杯」アイコンに onPrimary tint を適用。DrinkButton、ボトムナビ、設定、非対応画面のアイコンには既に tint があった。

### ビルド・テスト結果
- `./gradlew assembleDebug testDebugUnitTest lint`（サンドボックス外実行）— **BUILD SUCCESSFUL**。
- assembleDebug、testDebugUnitTest、lint が完了。lint errors 0。

### セルフレビュー
- [x] AppButton の内容色を維持したまま、通常 icon に foreground tint を適用。
- [x] 無効な翌月ボタンは操作不可かつアイコン tint 35%。
- [x] 今日の枠判定は DayStatus と独立し、飲酒日状態と併存。
- [x] 凡例の飲酒マークは onSurface。
- [x] ビルド、unit tests、lint が成功。

### 未解決・判断したこと
- なし。
