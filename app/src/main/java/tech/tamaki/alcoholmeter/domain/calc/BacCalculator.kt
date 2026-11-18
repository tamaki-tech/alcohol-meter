package tech.tamaki.alcoholmeter.domain.calc

import java.time.Duration
import java.time.Instant
import tech.tamaki.alcoholmeter.domain.model.DrinkLog
import tech.tamaki.alcoholmeter.domain.model.Sex
import tech.tamaki.alcoholmeter.domain.model.UserProfile
import kotlin.math.max

/** BAC [%] = 純アルコール [g] / (体重 [kg] × r × 10)、分解は β [%/時]。 */
object BacCalculator {
    fun bacAt(logs: List<DrinkLog>, profile: UserProfile, at: Instant): Double {
        require(profile.weightKg > 0 && profile.eliminationRatePerHour > 0)
        var bac = 0.0
        var previous: Instant? = null
        for (log in logs.filter { !it.drankAt.isAfter(at) }.sortedBy { it.drankAt }) {
            previous?.let {
                bac = max(0.0, bac - hours(it, log.drankAt) * profile.eliminationRatePerHour)
            }
            bac += log.pureAlcoholG / (profile.weightKg * (if (profile.sex == Sex.MALE) 0.68 else 0.55) * 10)
            previous = log.drankAt
        }
        return previous?.let { max(0.0, bac - hours(it, at) * profile.eliminationRatePerHour) } ?: 0.0
    }
    private fun hours(a: Instant, b: Instant) = Duration.between(a, b).toMillis() / 3_600_000.0
    fun soberAt(logs: List<DrinkLog>, profile: UserProfile, at: Instant): Instant? {
        val bac = bacAt(logs, profile, at)
        return if (bac <= 0) null else at.plusNanos(kotlin.math.ceil(bac / profile.eliminationRatePerHour * 3_600_000_000_000.0).toLong())
    }
    fun series(logs: List<DrinkLog>, profile: UserProfile, step: Duration = Duration.ofMinutes(5)): List<Pair<Instant, Double>> {
        require(!step.isZero && !step.isNegative)
        val start = logs.minOfOrNull { it.drankAt } ?: return emptyList()
        val last = logs.maxOf { it.drankAt }
        val end = soberAt(logs, profile, last) ?: last
        val result = mutableListOf<Pair<Instant, Double>>()
        var t = start
        while (t.isBefore(end)) {
            result.add(t to bacAt(logs, profile, t))
            t = t.plus(step)
        }
        result.add(end to bacAt(logs, profile, end))
        return result
    }
    fun peak(logs: List<DrinkLog>, profile: UserProfile): Double = logs.maxOfOrNull { bacAt(logs, profile, it.drankAt) } ?: 0.0
}
