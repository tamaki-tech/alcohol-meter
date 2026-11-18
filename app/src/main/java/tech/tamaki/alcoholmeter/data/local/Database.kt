package tech.tamaki.alcoholmeter.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import tech.tamaki.alcoholmeter.domain.model.HcSyncStatus

@Entity(tableName = "party_session")
data class SessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Instant,
    val startOffset: ZoneOffset,
    val endedAt: Instant?,
    val endOffset: ZoneOffset?,
    val title: String?,
    val hcSyncStatus: HcSyncStatus,
)

@Entity(tableName = "drink_log", foreignKeys = [ForeignKey(entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)], indices = [Index("sessionId")])
data class LogEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val drankAt: Instant,
    val offset: ZoneOffset,
    val name: String,
    val beverageType: Int,
    val volumeMl: Double,
    val abvPercent: Double,
    val pureAlcoholG: Double,
    val colorIndex: Int,
    val hcRecordId: String?,
    val hcVersion: Long = 1,
    @ColumnInfo(name = "preset_id") val presetId: Long? = null,
)

@Entity(tableName = "drink_preset")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val shortName: String,
    val beverageType: Int,
    val volumeMl: Double,
    val abvPercent: Double,
    val icon: String,
    val colorIndex: Int,
    val sortOrder: Int,
    val showInNotification: Boolean,
)

@Entity(tableName = "hc_pending_deletion")
data class PendingDeletionEntity(@PrimaryKey @ColumnInfo(name = "client_record_id") val clientRecordId: String)

data class SessionAndLogs(
    @Embedded val session: SessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId") val logs: List<LogEntity>,
)

class Converters {
    @TypeConverter fun instant(value: Instant?): String? = value?.toString()
    @TypeConverter fun instant(value: String?): Instant? = value?.let(Instant::parse)
    @TypeConverter fun offset(value: ZoneOffset?): Int? = value?.totalSeconds
    @TypeConverter fun offset(value: Int?): ZoneOffset? = value?.let(ZoneOffset::ofTotalSeconds)
    @TypeConverter fun status(value: HcSyncStatus): String = value.name
    @TypeConverter fun status(value: String): HcSyncStatus = HcSyncStatus.valueOf(value)
}

@Dao
interface PartyDao {
    @Query("SELECT * FROM party_session WHERE endedAt IS NULL LIMIT 1") suspend fun active(): SessionEntity?
    @Transaction @Query("SELECT * FROM party_session WHERE endedAt IS NULL LIMIT 1") fun observeActive(): Flow<SessionAndLogs?>
    @Transaction @Query("SELECT * FROM party_session WHERE id = :id") fun observeSession(id: String): Flow<SessionAndLogs?>
    @Transaction @Query("SELECT * FROM party_session WHERE endedAt IS NOT NULL ORDER BY startedAt DESC") fun observeHistory(): Flow<List<SessionAndLogs>>
    @Query("SELECT * FROM drink_log ORDER BY drankAt") fun observeLogs(): Flow<List<LogEntity>>
    @Query("SELECT * FROM drink_log WHERE id = :id") suspend fun log(id: String): LogEntity?
    @Query("SELECT * FROM drink_log WHERE sessionId = :id") suspend fun logsForSession(id: String): List<LogEntity>
    @Query("SELECT * FROM party_session WHERE id = :id") suspend fun session(id: String): SessionEntity?
    @Query("SELECT * FROM hc_pending_deletion") suspend fun pendingDeletions(): List<PendingDeletionEntity>
    @Query("SELECT id FROM party_session WHERE endedAt IS NOT NULL AND hcSyncStatus != 'SYNCED'") suspend fun pendingSessionIds(): List<String>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun queueDeletion(item: PendingDeletionEntity)
    @Query("DELETE FROM hc_pending_deletion WHERE client_record_id = :id") suspend fun clearDeletion(id: String)
    @Insert suspend fun insertSession(session: SessionEntity)
    @Upsert suspend fun saveSession(session: SessionEntity)
    @Upsert suspend fun saveLog(log: LogEntity)
    @Query("DELETE FROM drink_log WHERE id = :id") suspend fun deleteLog(id: String)
    @Query("DELETE FROM party_session WHERE id = :id") suspend fun deleteSession(id: String)
}

@Dao
interface PresetDao {
    @Query("SELECT * FROM drink_preset ORDER BY sortOrder, id") fun observe(): Flow<List<PresetEntity>>
    @Query("SELECT * FROM drink_preset ORDER BY sortOrder, id") suspend fun all(): List<PresetEntity>
    @Query("SELECT * FROM drink_preset WHERE id = :id") suspend fun get(id: Long): PresetEntity?
    @Upsert suspend fun upsert(preset: PresetEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(presets: List<PresetEntity>)
    @Query("DELETE FROM drink_preset WHERE id = :id") suspend fun delete(id: Long)
}

@Database(entities = [SessionEntity::class, LogEntity::class, PresetEntity::class, PendingDeletionEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AlcoholDatabase : RoomDatabase() {
    abstract fun partyDao(): PartyDao
    abstract fun presetDao(): PresetDao
}
