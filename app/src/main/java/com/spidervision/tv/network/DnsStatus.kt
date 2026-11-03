package com.spidervision.tv.network

import android.content.Context
import android.provider.Settings

/**
 * What the TV's own Private DNS setting says. Read-only: SpiderVision never changes DNS.
 * Live NextDNS statistics are not connected yet (Phase 4), so nothing here is a count.
 */
data class DnsStatus(val title: String, val detail: String)

object DnsStatusReader {
    fun read(context: Context): DnsStatus {
        val cr = context.contentResolver
        val mode = runCatching { Settings.Global.getString(cr, "private_dns_mode") }.getOrNull()
        val host = runCatching { Settings.Global.getString(cr, "private_dns_specifier") }.getOrNull()
        return when {
            mode == "hostname" && host?.contains("nextdns", ignoreCase = true) == true ->
                DnsStatus("NextDNS on", "Private DNS points at NextDNS.")
            mode == "hostname" -> DnsStatus("Private DNS on", "A custom Private DNS provider is set.")
            mode == "opportunistic" -> DnsStatus("Private DNS auto", "The TV uses encrypted DNS when available.")
            mode == "off" -> DnsStatus("Private DNS off", "The TV uses the network's DNS.")
            else -> DnsStatus("DNS status unknown", "The TV didn't report its Private DNS mode.")
        }
    }
}
