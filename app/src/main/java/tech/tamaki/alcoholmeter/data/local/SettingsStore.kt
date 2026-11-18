package tech.tamaki.alcoholmeter.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.map
import tech.tamaki.alcoholmeter.domain.calc.AbstinenceCalculator
import tech.tamaki.alcoholmeter.domain.model.Sex
import tech.tamaki.alcoholmeter.domain.model.UserProfile
import tech.tamaki.alcoholmeter.domain.repository.SettingsRepository

class SettingsStore(private val store: DataStore<Preferences>, private val clock: Clock) : SettingsRepository {
    private object Keys {
        val weight = doublePreferencesKey("weight_kg")
        val sex = stringPreferencesKey("sex")
        val rate = doublePreferencesKey("elimination_rate")
        val start = stringPreferencesKey("record_start_date")
        val finished = stringPreferencesKey("last_finished_session_id")
        val onboarding = booleanPreferencesKey("onboarding_completed")
    }
    override val profile = store.data.map { p ->
        val weight = p[Keys.weight]
        val sex = p[Keys.sex]
        if (weight == null || sex == null) null else UserProfile(weight, Sex.valueOf(sex), p[Keys.rate] ?: 0.015)
    }
    override val onboardingCompleted = store.data.map { it[Keys.onboarding] ?: false }
    override val recordStartDate = store.data.map { it[Keys.start]?.let(LocalDate::parse) }
    val lastFinishedSessionId = store.data.map { it[Keys.finished] }
    override suspend fun saveProfile(p: UserProfile) {
        require(p.weightKg > 0 && p.eliminationRatePerHour > 0)
        store.edit { it[Keys.weight] = p.weightKg; it[Keys.sex] = p.sex.name; it[Keys.rate] = p.eliminationRatePerHour }
    }
    override suspend fun completeOnboarding() {
        val now = clock.instant()
        store.edit {
            if (it[Keys.start] == null) it[Keys.start] = AbstinenceCalculator.logicalDate(now, clock.zone.rules.getOffset(now)).toString()
            it[Keys.onboarding] = true
        }
    }
    suspend fun setFinished(id: String?) {
        store.edit { if (id == null) it.remove(Keys.finished) else it[Keys.finished] = id }
    }
}
