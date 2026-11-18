package tech.tamaki.alcoholmeter.ui.viewmodel

import android.content.Context
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.ui.format.durationParts

internal fun durationText(context: Context, seconds: Long): String {
    val parts = durationParts(seconds)
    return if (parts.hours == 0L) context.getString(R.string.vm_duration_minutes, parts.minutes)
    else context.getString(R.string.vm_duration_hours_minutes, parts.hours, parts.minutes)
}
