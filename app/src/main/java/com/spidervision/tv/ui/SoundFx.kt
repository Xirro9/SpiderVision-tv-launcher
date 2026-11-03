package com.spidervision.tv.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import com.spidervision.tv.R

/**
 * Short UI sounds (synthesized by tools/make_sounds.py, ~37 KB in total), played through one small
 * SoundPool. Quiet by default, rate-limited for held D-pad keys, and switchable per profile.
 */
object SoundFx {
    enum class Cue { TICK, THWIP, BACK, SENSE, BUZZ }

    private var pool: SoundPool? = null
    private val ids = IntArray(Cue.entries.size)
    private var lastTick = 0L
    private var quietUntil = 0L

    /** Off for the current profile → nothing plays. */
    @Volatile var enabled = true

    fun init(context: Context) {
        if (pool != null) return
        pool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).build().also { p ->
                ids[Cue.TICK.ordinal] = p.load(context, R.raw.sv_tick, 1)
                ids[Cue.THWIP.ordinal] = p.load(context, R.raw.sv_thwip, 1)
                ids[Cue.BACK.ordinal] = p.load(context, R.raw.sv_back, 1)
                ids[Cue.SENSE.ordinal] = p.load(context, R.raw.sv_sense, 1)
                ids[Cue.BUZZ.ordinal] = p.load(context, R.raw.sv_buzz, 1)
            }
    }

    fun release() {
        pool?.release()
        pool = null
    }

    /** Suppress ticks for a moment, e.g. while focus is restored automatically. */
    fun hush(ms: Long = 400) {
        quietUntil = SystemClock.uptimeMillis() + ms
    }

    fun play(cue: Cue) {
        if (!enabled) return
        val p = pool ?: return
        val now = SystemClock.uptimeMillis()
        if (cue == Cue.TICK) {
            if (now < quietUntil || now - lastTick < 55) return // held keys: at most ~18 ticks/s
            lastTick = now
        }
        val volume = when (cue) { Cue.TICK -> 0.18f; Cue.SENSE -> 0.35f; else -> 0.3f }
        p.play(ids[cue.ordinal], volume, volume, 1, 0, 1f)
    }
}
