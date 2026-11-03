package com.spidervision.tv.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Links or unlinks NextDNS from a PC over ADB (see tools/link-nextdns.ps1).
 *
 * The manifest requires the sender to hold WRITE_SECURE_SETTINGS, which the ADB shell has and
 * ordinary apps cannot get, so no other app on the TV can set or clear the key. The key is
 * format-checked, encrypted and never echoed back or logged.
 */
class ProvisionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = when {
            intent.getBooleanExtra(EXTRA_CLEAR, false) -> {
                KeyVault.clear(context)
                NetworkStatusCache.clear(context)
                "unlinked"
            }
            intent.hasExtra(EXTRA_KEY) -> {
                val key = intent.getStringExtra(EXTRA_KEY)?.trim().orEmpty()
                if (KEY_FORMAT.matches(key)) {
                    KeyVault.save(context, key)
                    NetworkStatusCache.clear(context)
                    "linked"
                } else "rejected: that doesn't look like a NextDNS API key"
            }
            else -> "nothing to do"
        }
        resultData = result
    }

    companion object {
        const val EXTRA_KEY = "nextdns_key"
        const val EXTRA_CLEAR = "nextdns_clear"
        private val KEY_FORMAT = Regex("^[A-Za-z0-9_-]{16,128}$")
    }
}
