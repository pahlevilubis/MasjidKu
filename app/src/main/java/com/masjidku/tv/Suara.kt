package com.masjidku.tv

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

/** Pemutar rekaman adzan/tarhim dan bunyi beep. */
object Suara {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pemutar: MediaPlayer? = null
    private var tugasBeep: Job? = null
    @Volatile var sedangMemutar = ""
        private set

    @Synchronized
    fun putar(berkas: File, volume: Int, nama: String): Boolean {
        berhenti()
        return try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setDataSource(berkas.absolutePath)
            mp.prepare()
            val v = volume.coerceIn(0, 100) / 100f
            mp.setVolume(v, v)
            mp.setOnCompletionListener { selesai(it) }
            mp.setOnErrorListener { p, _, _ -> selesai(p); true }
            mp.start()
            pemutar = mp
            sedangMemutar = nama
            true
        } catch (e: Exception) {
            Log.e("MasjidKu", "Gagal memutar ${berkas.name}", e)
            false
        }
    }

    @Synchronized
    private fun selesai(mp: MediaPlayer) {
        try { mp.release() } catch (_: Exception) {}
        if (pemutar === mp) {
            pemutar = null
            sedangMemutar = ""
        }
    }

    /** Putar rekaman dari slot; false bila berkas tidak ada / gagal. */
    fun putarSlot(slot: String, volume: Int): Boolean {
        val f = Repo.berkasAudio(slot) ?: return false
        return putar(f, volume, slot)
    }

    fun beep(jumlah: Int, panjangMs: Int, volume: Int) {
        tugasBeep?.cancel()
        tugasBeep = scope.launch {
            val data = buatNada(1000.0, panjangMs)
            repeat(jumlah) {
                mainkan(data, panjangMs, volume)
                delay(250L)
            }
        }
    }

    @Synchronized
    fun berhenti() {
        tugasBeep?.cancel()
        pemutar?.let {
            try { it.stop() } catch (_: Exception) {}
            try { it.release() } catch (_: Exception) {}
        }
        pemutar = null
        sedangMemutar = ""
    }

    private fun buatNada(frek: Double, ms: Int): ShortArray {
        val rate = 44100
        val n = rate * ms / 1000
        val pudar = rate / 100
        val out = ShortArray(n)
        for (i in 0 until n) {
            var amp = 1.0
            if (i < pudar) amp = i / pudar.toDouble()
            if (i > n - pudar) amp = (n - i) / pudar.toDouble()
            out[i] = (sin(2 * PI * frek * i / rate) * amp * 0.8 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private suspend fun mainkan(data: ShortArray, ms: Int, volume: Int) {
        var t: AudioTrack? = null
        try {
            @Suppress("DEPRECATION")
            val trek = AudioTrack(
                AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, data.size * 2, AudioTrack.MODE_STATIC
            )
            t = trek
            trek.write(data, 0, data.size)
            trek.setVolume(volume.coerceIn(0, 100) / 100f)
            trek.play()
            delay(ms + 60L)
        } catch (e: Exception) {
            Log.w("MasjidKu", "Beep gagal: ${e.message}")
        } finally {
            try { t?.stop() } catch (_: Exception) {}
            try { t?.release() } catch (_: Exception) {}
        }
    }
}
