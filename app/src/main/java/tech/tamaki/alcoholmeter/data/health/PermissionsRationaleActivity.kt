package tech.tamaki.alcoholmeter.data.health

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import tech.tamaki.alcoholmeter.R

class PermissionsRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = getString(R.string.hc_permissions_rationale)
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        })
    }
}
