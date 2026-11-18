package tech.tamaki.alcoholmeter.background

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import tech.tamaki.alcoholmeter.MainActivity
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.domain.calc.BacCalculator
import tech.tamaki.alcoholmeter.domain.model.AppState
import tech.tamaki.alcoholmeter.domain.repository.PartyRepository
import tech.tamaki.alcoholmeter.domain.repository.PresetRepository
import tech.tamaki.alcoholmeter.domain.repository.SettingsRepository

@Singleton
class PartyNotificationController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val party: PartyRepository,
    private val presets: PresetRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    private val manager get() = NotificationManagerCompat.from(context)
    private fun allowed() = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    private fun channel() {
        val system = context.getSystemService(NotificationManager::class.java)
        system.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.notif_channel_party), NotificationManager.IMPORTANCE_LOW))
        system.createNotificationChannel(NotificationChannel(REMINDER_CHANNEL, context.getString(R.string.notif_channel_reminder), NotificationManager.IMPORTANCE_DEFAULT))
    }
    private fun openApp(): PendingIntent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun openReminder(): PendingIntent = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java).putExtra("open_end_dialog", true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun action(action: String, request: Int, configure: Intent.() -> Unit = {}): PendingIntent =
        PendingIntent.getBroadcast(context, request, Intent(context, DrinkActionReceiver::class.java).setAction(action).apply(configure), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    suspend fun refresh() {
        val state = party.observeAppState().first() as? AppState.InParty
        if (state == null) { manager.cancel(PARTY_ID); return }
        if (!allowed()) return
        channel()
        val profile = settings.profile.first()
        val bac = profile?.let { BacCalculator.bacAt(state.data.logs, it, clock.instant()) } ?: 0.0
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_beer)
            .setContentTitle(context.getString(R.string.notif_party_title))
            .setContentText(context.getString(R.string.notif_party_body, state.data.logs.size, bac))
            .setContentIntent(openApp()).setWhen(state.data.session.startedAt.toEpochMilli())
            .setUsesChronometer(true).setOnlyAlertOnce(true).setAutoCancel(false)
        presets.observePresets().first().filter { it.showInNotification }.take(3).forEach { preset ->
            builder.addAction(0, preset.shortName, action(DrinkActionReceiver.ACTION_DRINK, preset.id.toInt()) { putExtra("presetId", preset.id) })
        }
        try { manager.notify(PARTY_ID, builder.build()) } catch (_: SecurityException) { /* 権限が検査後に取り消された。 */ }
    }
    fun showReminder(last: Instant) {
        if (!allowed()) return
        channel()
        val label = DateTimeFormatter.ofPattern("HH:mm").withZone(clock.zone).format(last)
        val end = action(DrinkActionReceiver.ACTION_END, 100) { putExtra("lastDrink", last.toString()) }
        val keep = action(DrinkActionReceiver.ACTION_CONTINUE, 101)
        try { manager.notify(REMINDER_ID, NotificationCompat.Builder(context, REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_beer)
            .setContentTitle(context.getString(R.string.end_question))
            .setContentText(context.getString(R.string.notif_reminder_body, label))
            .setContentIntent(openReminder())
            .addAction(0, context.getString(R.string.notif_end_at, label), end)
            .addAction(0, context.getString(R.string.notif_continue), keep)
            .setAutoCancel(true).build()) } catch (_: SecurityException) { /* 権限が検査後に取り消された。 */ }
    }
    fun cancelReminder() = manager.cancel(REMINDER_ID)
    companion object {
        private const val CHANNEL = "party"
        private const val REMINDER_CHANNEL = "reminder"
        private const val PARTY_ID = 1001
        private const val REMINDER_ID = 1002
    }
}
