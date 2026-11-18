package tech.tamaki.alcoholmeter.data.health

import javax.inject.Inject
import tech.tamaki.alcoholmeter.data.local.AlcoholDatabase
import tech.tamaki.alcoholmeter.domain.usecase.PendingHealthSync

class RoomPendingHealthSync @Inject constructor(private val db: AlcoholDatabase) : PendingHealthSync {
    override suspend fun pendingSessionIds() = db.partyDao().pendingSessionIds()
    override suspend fun hasPendingDeletions() = db.partyDao().pendingDeletions().isNotEmpty()
}
