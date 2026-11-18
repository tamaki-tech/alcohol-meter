package tech.tamaki.alcoholmeter.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.tamaki.alcoholmeter.domain.model.DrinkPreset
import tech.tamaki.alcoholmeter.domain.repository.PresetRepository
import tech.tamaki.alcoholmeter.domain.usecase.ObservePresets
import tech.tamaki.alcoholmeter.domain.usecase.SavePreset

@OptIn(ExperimentalCoroutinesApi::class)
class PresetEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @BeforeEach fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun cleanup() { Dispatchers.resetMain() }
    @Test fun notificationLimitBlocksFourthPreset() = runTest(dispatcher) {
        val repository = object : PresetRepository {
            val presets = MutableStateFlow((1L..3L).map { DrinkPreset(it, "d$it", "d", 1, 350.0, 5.0, "beer", 0, it.toInt(), true) })
            override fun observePresets() = presets
            override suspend fun upsert(p: DrinkPreset) { presets.value = presets.value + p }
            override suspend fun reorder(idsInOrder: List<Long>) = Unit
            override suspend fun delete(id: Long) = Unit
        }
        val vm = PresetEditorViewModel(SavedStateHandle(mapOf("presetId" to 0L)), ObservePresets(repository), SavePreset(repository))
        vm.uiState.test {
            awaitItem()
            val (state, draft) = awaitItem()
            assertTrue(state.notificationLimitReached)
            vm.edit(draft.copy(notificationEnabled = true))
            runCurrent()
            assertFalse(vm.uiState.value.second.notificationEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
