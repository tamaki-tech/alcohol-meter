package tech.tamaki.alcoholmeter.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import tech.tamaki.alcoholmeter.background.PartyEffectsImpl
import tech.tamaki.alcoholmeter.domain.usecase.PartyEffects
import tech.tamaki.alcoholmeter.data.health.RoomPendingHealthSync
import tech.tamaki.alcoholmeter.domain.usecase.PendingHealthSync
import tech.tamaki.alcoholmeter.data.health.RoomHealthSyncOperation
import tech.tamaki.alcoholmeter.domain.usecase.HealthSyncOperation
import tech.tamaki.alcoholmeter.data.health.HealthConnectRepositoryImpl
import tech.tamaki.alcoholmeter.data.health.HealthSyncGateway
import tech.tamaki.alcoholmeter.domain.repository.HealthConnectRepository
import tech.tamaki.alcoholmeter.data.local.AlcoholDatabase
import tech.tamaki.alcoholmeter.data.local.SettingsStore
import tech.tamaki.alcoholmeter.data.repository.RoomPartyRepository
import tech.tamaki.alcoholmeter.data.repository.RoomPresetRepository
import tech.tamaki.alcoholmeter.domain.repository.PartyRepository
import tech.tamaki.alcoholmeter.domain.repository.PresetRepository
import tech.tamaki.alcoholmeter.domain.repository.SettingsRepository

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton fun clock(): Clock = object : Clock() {
        private val utc = Clock.systemUTC()
        override fun getZone(): ZoneId = ZoneId.systemDefault()
        override fun withZone(zone: ZoneId): Clock = utc.withZone(zone)
        override fun instant(): Instant = utc.instant()
    }
    @Provides @IoDispatcher fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO
    @Provides @Singleton fun preferences(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }
    @Provides @Singleton fun settings(store: DataStore<Preferences>, clock: Clock) = SettingsStore(store, clock)
    @Provides fun settingsRepository(store: SettingsStore): SettingsRepository = store
    @Provides @Singleton fun database(@ApplicationContext context: Context): AlcoholDatabase =
        Room.databaseBuilder(context, AlcoholDatabase::class.java, "alcohol-meter.db")
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS one_active_session ON party_session ((1)) WHERE endedAt IS NULL")
                    val sql = "INSERT INTO drink_preset (id,name,shortName,beverageType,volumeMl,abvPercent,icon,colorIndex,sortOrder,showInNotification) VALUES (?,?,?,?,?,?,?,?,?,?)"
                    listOf(
                        arrayOf<Any>(1, "ビール（中ジョッキ）", "ビール", 1, 350, 5, "beer", 0, 0, 1),
                        arrayOf<Any>(2, "ハイボール", "ハイボール", 18, 350, 7, "glass-water", 1, 1, 1),
                        arrayOf<Any>(3, "日本酒（1合）", "日本酒", 10, 180, 15, "milk", 2, 2, 1),
                        arrayOf<Any>(4, "ワイン（グラス）", "ワイン", 2, 120, 12, "wine", 3, 3, 0),
                        arrayOf<Any>(5, "レモンサワー", "レモン", 17, 350, 5, "citrus", 4, 4, 0),
                        arrayOf<Any>(6, "焼酎ロック", "焼酎", 11, 60, 25, "shochu", 5, 5, 0),
                    ).forEach { db.execSQL(sql, it) }
                }
            }).build()
    @Provides fun pendingHealthSync(impl: RoomPendingHealthSync): PendingHealthSync = impl
    @Provides fun partyEffects(impl: PartyEffectsImpl): PartyEffects = impl
    @Provides fun healthSyncOperation(impl: RoomHealthSyncOperation): HealthSyncOperation = impl
    @Provides fun healthSyncGateway(impl: HealthConnectRepositoryImpl): HealthSyncGateway = impl
    @Provides fun healthRepository(impl: HealthConnectRepositoryImpl): HealthConnectRepository = impl
    @Provides @Singleton fun partyRepository(db: AlcoholDatabase, settings: SettingsStore, clock: Clock): PartyRepository = RoomPartyRepository(db, settings, clock)
    @Provides @Singleton fun presetRepository(db: AlcoholDatabase): PresetRepository = RoomPresetRepository(db)
}
