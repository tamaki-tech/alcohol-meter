package tech.tamaki.alcoholmeter.ui.navigation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.tamaki.alcoholmeter.ui.components.AppButton
import tech.tamaki.alcoholmeter.ui.components.AppButtonStyle
import tech.tamaki.alcoholmeter.domain.model.UserProfile
import tech.tamaki.alcoholmeter.domain.model.Sex
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavGraphBuilder
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.ui.components.AppBottomNav
import tech.tamaki.alcoholmeter.ui.screens.calendar.CalendarScreen
import tech.tamaki.alcoholmeter.ui.screens.history.HistoryDetailScreen
import tech.tamaki.alcoholmeter.ui.screens.history.HistoryListScreen
import tech.tamaki.alcoholmeter.ui.screens.home.HomeBeforeScreen
import tech.tamaki.alcoholmeter.ui.screens.home.HomeInPartyScreen
import tech.tamaki.alcoholmeter.ui.screens.onboarding.OnboardingScreen
import tech.tamaki.alcoholmeter.ui.screens.settings.PresetEditorScreen
import tech.tamaki.alcoholmeter.ui.screens.settings.SettingsScreen
import tech.tamaki.alcoholmeter.ui.screens.summary.EndPartyConfirmation
import tech.tamaki.alcoholmeter.ui.screens.summary.SessionSummaryScreen
import tech.tamaki.alcoholmeter.ui.screens.unsupported.UnsupportedScreen
import tech.tamaki.alcoholmeter.ui.screens.unsupported.UnsupportedUiState
import tech.tamaki.alcoholmeter.ui.viewmodel.*

@Composable
fun AlcoholMeterNavigation(openEndDialog: Boolean = false, onEndDialogOpened: () -> Unit = {}) {
    val onboarding: OnboardingViewModel = hiltViewModel()
    val launch by onboarding.launchState.collectAsState()
    val onboardingState by onboarding.uiState.collectAsState()
    val healthContract = remember { onboarding.permissionContract() }
    val healthPermission = rememberLauncherForActivityResult(healthContract) { onboarding.healthResult() }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onboarding.finish() }
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
    when (launch) {
        LaunchState.Loading -> Unit
        LaunchState.Unsupported -> UnsupportedScreen(UnsupportedUiState(stringResource(R.string.vm_unsupported_explanation)))
        LaunchState.Onboarding -> OnboardingScreen(onboardingState, onboarding::setWeight, onboarding::setSex, onboarding::next, onboarding::skip, { healthPermission.launch(onboarding.permissions) }, { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) })
        LaunchState.Ready -> MainNavigation(openEndDialog, onEndDialogOpened)
    }
    }
}

@Composable
private fun MainNavigation(openEndDialog: Boolean, onEndDialogOpened: () -> Unit) {
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route ?: "home"
    LaunchedEffect(openEndDialog, current) { if (openEndDialog && current != "home") nav.navigate("home") { launchSingleTop = true } }
    Column(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.weight(1f),
            // 画面切り替えは即時（フェード等のモーションなし）
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            screen("home") {
                val vm: HomeViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()
                when (val home = state) {
                    HomeState.Loading -> Unit
                    is HomeState.Before -> HomeBeforeScreen(home.data, vm::start, { home.recentSessionId?.let { nav.navigate("detail/$it") } })
                    is HomeState.InParty -> {
                        LaunchedEffect(openEndDialog) { if (openEndDialog) { vm.showEndDialog(true); onEndDialogOpened() } }
                        HomeInPartyScreen(home.data, { vm.record(it.id) }, vm::undoLast, { vm.showEndDialog() })
                        home.confirmation?.let { EndPartyConfirmation(it, vm::confirmEnd, vm::dismissEndDialog) }
                    }
                    is HomeState.Finished -> LaunchedEffect(home.sessionId) { nav.navigate("summary/${home.sessionId}") { launchSingleTop = true } }
                }
            }
            screen("summary/{sessionId}") {
                val vm: SummaryViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()
                state?.let { SessionSummaryScreen(it, { vm.closeSummary { nav.navigate("home") { popUpTo("home") { inclusive = true }; launchSingleTop = true } } }, vm::retry, { nav.navigate("settings") }) }
            }
            screen("history") {
                val vm: HistoryListViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()
                HistoryListScreen(state, { nav.navigate("detail/${it.id}") }, { vm.retry(it.id) })
            }
            screen("detail/{sessionId}") {
                val vm: HistoryDetailViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()
                state?.let { HistoryDetailScreen(it, { nav.popBackStack() }, vm::edit, vm::save, vm::deleteDrink, { vm.deleteSession { nav.popBackStack() } }, vm::retry, { nav.navigate("settings") }, vm::cancelEdit) }
            }
            screen("calendar") {
                val vm: CalendarViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()
                CalendarScreen(state, vm::selectMonth) { date -> vm.sessionForDay(date)?.let { nav.navigate("detail/$it") } }
            }
            screen("settings") {
                val vm: SettingsViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()
                val profile by vm.profile.collectAsState()
                var editing by remember { mutableStateOf(false) }
                val contract = remember { vm.permissionContract() }
                val launcher = rememberLauncherForActivityResult(contract) { vm.refreshHealth() }
                SettingsScreen(state, { nav.navigate("preset/${it.id}") }, { nav.navigate("preset/0") }, vm::move, { editing = true }, { launcher.launch(vm.permissions) })
                if (editing && profile != null) ProfileDialog(profile!!, { vm.updateProfile(it); editing = false }, { editing = false })
            }
            screen("preset/{presetId}", arguments = listOf(navArgument("presetId") { type = NavType.LongType })) {
                val vm: PresetEditorViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()
                PresetEditorScreen(state.first, state.second, vm::edit, { vm.save { nav.popBackStack() } }, { nav.popBackStack() })
            }
        }
        if (current in setOf("home", "history", "calendar", "settings")) {
            AppBottomNav(current, onSelect = { route -> nav.navigate(route) { launchSingleTop = true; popUpTo("home") { saveState = true }; restoreState = true } })
        }
    }
}

@Composable
private fun ProfileDialog(profile: UserProfile, save: (UserProfile) -> Unit, dismiss: () -> Unit) {
    var weight by remember(profile) { mutableStateOf(profile.weightKg.toString()) }
    var rate by remember(profile) { mutableStateOf(profile.eliminationRatePerHour.toString()) }
    var sex by remember(profile) { mutableStateOf(profile.sex) }
    val parsedWeight = weight.toDoubleOrNull()
    val parsedRate = rate.toDoubleOrNull()
    val valid = parsedWeight != null && parsedWeight.isFinite() && parsedWeight > 0 && parsedRate != null && parsedRate.isFinite() && parsedRate > 0
    AlertDialog(onDismissRequest = dismiss, title = { Text(stringResource(R.string.settings_bac_calculation)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 数値は数字キーボードで入力し、単位を常に表示する
            OutlinedTextField(weight, { weight = it }, label = { Text(stringResource(R.string.settings_weight)) }, suffix = { Text(stringResource(R.string.unit_kg)) }, singleLine = true, isError = parsedWeight == null || parsedWeight <= 0, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            // 性別はトグルの文字ではなく、選択中が分かる 2 択にする
            Text(stringResource(R.string.settings_sex), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Sex.MALE to R.string.onboarding_male, Sex.FEMALE to R.string.onboarding_female).forEach { (value, label) ->
                    AppButton(stringResource(label), { sex = value }, Modifier.weight(1f).semantics { selected = sex == value }, if (sex == value) AppButtonStyle.Primary else AppButtonStyle.Secondary, 48.dp)
                }
            }
            OutlinedTextField(rate, { rate = it }, label = { Text(stringResource(R.string.settings_elimination_rate)) }, suffix = { Text(stringResource(R.string.unit_bac_per_hour)) }, singleLine = true, isError = parsedRate == null || parsedRate <= 0, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        }
    }, confirmButton = { TextButton(onClick = { if (valid) save(UserProfile(parsedWeight!!, sex, parsedRate!!)) }, enabled = valid) { Text(stringResource(R.string.save)) } }, dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } })
}

/**
 * 背景色を敷いた画面。遷移アニメーションを無効にしていても、切り替えの瞬間に新旧の画面が
 * 1〜数フレーム同時に描かれるため、背景を不透明にして新しい画面で古い画面を完全に隠す。
 */
private fun NavGraphBuilder.screen(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable(route, arguments) { entry ->
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content(entry) }
    }
}
