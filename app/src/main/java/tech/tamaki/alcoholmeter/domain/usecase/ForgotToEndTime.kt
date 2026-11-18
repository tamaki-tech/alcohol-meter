package tech.tamaki.alcoholmeter.domain.usecase

import java.time.Duration
import java.time.Instant

/** 最後の飲酒がない場合は通知しない。 */
fun forgotToEndAt(lastDrinkAt: Instant?): Instant? = lastDrinkAt?.plus(Duration.ofHours(6))

fun shouldRemindToEnd(lastDrinkAt: Instant?, now: Instant): Boolean =
    forgotToEndAt(lastDrinkAt)?.let { !now.isBefore(it) } ?: false
