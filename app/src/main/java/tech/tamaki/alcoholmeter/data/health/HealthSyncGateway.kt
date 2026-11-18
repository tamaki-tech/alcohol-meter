package tech.tamaki.alcoholmeter.data.health

import tech.tamaki.alcoholmeter.domain.model.DrinkLog

interface HealthSyncGateway {
    suspend fun canWriteAlcohol(): Boolean
    suspend fun insert(logs: List<DrinkLog>): Map<String, String>
    suspend fun delete(ids: List<String>)
}
