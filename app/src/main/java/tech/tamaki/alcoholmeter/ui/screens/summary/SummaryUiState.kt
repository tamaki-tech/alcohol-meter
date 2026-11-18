package tech.tamaki.alcoholmeter.ui.screens.summary

data class SessionMetricUiState(val label: String, val value: String)

data class DrinkCategoryUiState(val label: String, val count: Int, val colorIndex: Int)

data class BacPointUiState(val timeLabel: String, val bac: Float, val colorIndex: Int? = null)

data class DrinkRowUiState(val time: String, val name: String, val alcohol: String, val colorIndex: Int, val id: String = "")

enum class HealthSyncUiStatus { Sent, Failed, NotLinked }

data class SummaryUiState(
    val dateTitle: String,
    val metrics: List<SessionMetricUiState>,
    val categories: List<DrinkCategoryUiState>,
    val bacPoints: List<BacPointUiState>,
    val maximumBac: String,
    val soberEta: String,
    val drinkRows: List<DrinkRowUiState>,
    val syncStatus: HealthSyncUiStatus,
    val syncedCount: Int = 0,
)

data class EndPartyUiState(
    val cups: Int,
    val duration: String,
    val nowLabel: String,
    val lastDrinkLabel: String,
    val initialSelection: EndTimeChoice,
)

enum class EndTimeChoice { Now, LastDrink }
