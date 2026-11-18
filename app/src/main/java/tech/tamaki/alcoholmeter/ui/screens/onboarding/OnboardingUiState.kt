package tech.tamaki.alcoholmeter.ui.screens.onboarding

data class OnboardingUiState(
    val step: Int,
    val weightKg: String,
    val sex: OnboardingSex?,
    val healthConnectWeightDate: String?,
    val healthConnectGranted: Boolean,
)

enum class OnboardingSex { Male, Female }
