package com.spidervision.tv.network

import android.content.Context
import android.provider.Settings

/**
 * What the TV's own Private DNS setting says. Read-only: SpiderVision never changes DNS.
 * [nextDnsProfile] is the profile ID from a `<id>.dns.nextdns.io` hostname, used for stats.
 */
data class DnsStatus(val title: String, val detail: String, val nextDnsProfile: String? = null)

private val NEXTDNS_HOST = Regex("^([a-z0-9]+)\\.dns\\.nextdns\\.io$", RegexOption.IGNORE_CASE)

object DnsStatusReader {
    fun read(context: Context): DnsStatus {
        val cr = context.contentResolver
        val mode = runCatching { Settings.Global.getString(cr, "private_dns_mode") }.getOrNull()
        val host = runCatching { Settings.Global.getString(cr, "private_dns_specifier") }.getOrNull()
        val profile = host?.let { NEXTDNS_HOST.find(it.trim())?.groupValues?.get(1) }
        return when {
            mode == "hostname" && profile != null -> DnsStatus("NextDNS on", "Private DNS points at NextDNS.", profile)
            mode == "hostname" -> DnsStatus("Private DNS on", "A custom Private DNS provider is set.")
            mode == "opportunistic" -> DnsStatus("Private DNS auto", "The TV uses encrypted DNS when available.")
            mode == "off" -> DnsStatus("Private DNS off", "The TV uses the network's DNS.")
            else -> DnsStatus("DNS status unknown", "The TV didn't report its Private DNS mode.")
        }
    }
}

/** Everything the Home base tile needs, in one honest snapshot. */
data class NetView(
    val dns: DnsStatus,
    val linked: Boolean,
    val stats: NextDnsStats?,
    val error: String?,
    val authProblem: Boolean = false,
    val loading: Boolean = false,
)
