package com.spidervision.tv.network

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Last-24-hour totals for the TV's NextDNS profile (API: https://nextdns.github.io/api/). */
data class NextDnsStats(
    val queries: Long,
    val blocked: Long,
    /** Queries per hour for the last 12 hours, oldest first (empty if unavailable). */
    val hourly: List<Long>,
    /** Most-blocked domains; kept in memory only, never cached to disk or logged. */
    val topBlocked: List<Pair<String, Long>>,
    val fetchedAt: Long,
) {
    val blockedPercent: Double get() = if (queries == 0L) 0.0 else blocked * 100.0 / queries
}

sealed interface FetchResult {
    data class Ok(val stats: NextDnsStats) : FetchResult
    data class Failed(val message: String, val authProblem: Boolean = false) : FetchResult
}

object NextDnsClient {
    private const val BASE = "https://api.nextdns.io"

    /** Blocking network call; run on Dispatchers.IO. */
    fun fetch(profile: String, apiKey: String): FetchResult {
        return try {
            val status = get("/profiles/$profile/analytics/status?from=-24h", apiKey)
            var queries = 0L
            var blocked = 0L
            status.optJSONArray("data").forEachObject { o ->
                val n = o.optLong("queries")
                queries += n
                if (o.optString("status") == "blocked") blocked += n
            }
            // The chart and top domains are extras: if they fail, the totals still show.
            val hourly = runCatching {
                val s = get("/profiles/$profile/analytics/status;series?from=-12h&interval=3600", apiKey)
                var sums = LongArray(0)
                s.optJSONArray("data").forEachObject { o ->
                    val arr = o.optJSONArray("queries") ?: return@forEachObject
                    if (sums.size < arr.length()) sums = sums.copyOf(arr.length())
                    for (i in 0 until arr.length()) sums[i] += arr.optLong(i)
                }
                sums.toList().takeLast(12)
            }.getOrDefault(emptyList())
            val top = runCatching {
                val d = get("/profiles/$profile/analytics/domains?status=blocked&from=-24h&limit=4", apiKey)
                buildList { d.optJSONArray("data").forEachObject { add(it.optString("domain") to it.optLong("queries")) } }
            }.getOrDefault(emptyList())
            FetchResult.Ok(NextDnsStats(queries, blocked, hourly, top, System.currentTimeMillis()))
        } catch (e: HttpError) {
            when (e.code) {
                401, 403 -> FetchResult.Failed("NextDNS rejected the saved key", authProblem = true)
                404 -> FetchResult.Failed("NextDNS profile $profile wasn't found for this key", authProblem = true)
                429 -> FetchResult.Failed("NextDNS asked SpiderVision to slow down")
                else -> FetchResult.Failed("NextDNS returned an error (${e.code})")
            }
        } catch (e: IOException) {
            FetchResult.Failed("Couldn't reach NextDNS")
        } catch (e: org.json.JSONException) {
            FetchResult.Failed("NextDNS sent an unexpected reply")
        }
    }

    private class HttpError(val code: Int) : IOException("HTTP $code")

    private fun get(path: String, apiKey: String): JSONObject {
        val c = URL(BASE + path).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 5_000
            c.readTimeout = 7_000
            c.setRequestProperty("X-Api-Key", apiKey)
            c.setRequestProperty("Accept", "application/json")
            val code = c.responseCode
            if (code !in 200..299) throw HttpError(code)
            return JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        } finally {
            c.disconnect()
        }
    }

    private inline fun JSONArray?.forEachObject(block: (JSONObject) -> Unit) {
        if (this == null) return
        for (i in 0 until length()) optJSONObject(i)?.let(block)
    }
}

/** Last good totals, so the tile isn't blank after a restart or a network blip. No domains. */
object NetworkStatusCache {
    private const val PREFS = "netcache"

    fun save(context: Context, s: NextDnsStats) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("queries", s.queries).putLong("blocked", s.blocked)
            .putString("hourly", s.hourly.joinToString(","))
            .putLong("at", s.fetchedAt)
            .apply()
    }

    fun load(context: Context): NextDnsStats? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains("at")) return null
        return NextDnsStats(
            queries = p.getLong("queries", 0), blocked = p.getLong("blocked", 0),
            hourly = p.getString("hourly", "").orEmpty().split(',').mapNotNull { it.toLongOrNull() },
            topBlocked = emptyList(), fetchedAt = p.getLong("at", 0),
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }
}
