package tech.tamaki.alcoholmeter.domain.calc

/** 純アルコール量 [g] = 量 [ml] × 度数 [%] / 100 × 0.8。 */
object PureAlcohol {
    fun grams(volumeMl: Double, abvPercent: Double): Double = volumeMl * abvPercent / 100 * 0.8
}
