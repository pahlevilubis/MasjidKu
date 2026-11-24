package com.masjidku.tv

import android.os.SystemClock
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Sumber waktu MasjidKu.
 * - Bila internet ada: jam diambil langsung dari server waktu (NTP), tidak bergantung jam TV.
 * - Bila tidak ada internet: memakai jam TV + koreksi detik dari pengaturan.
 */
object Jam {
    private val SERVER = listOf("time.google.com", "id.pool.ntp.org", "pool.ntp.org", "time.cloudflare.com")
    private const val NTP_EPOCH = 2208988800L

    @Volatile private var basisNtp = 0L
    @Volatile private var basisElapsed = 0L
    @Volatile var ntpOk = false
        private set
    @Volatile var sumber = ""
        private set
    @Volatile var terakhirSinkron = 0L
        private set
    @Volatile var selisihJamTv = 0L
        private set

    fun sekarang(): Long {
        val c = Repo.cfg.value
        return if (ntpOk && c.pakaiNtp) {
            basisNtp + (SystemClock.elapsedRealtime() - basisElapsed)
        } else {
            System.currentTimeMillis() + c.koreksiJamDetik * 1000L
        }
    }

    /** Dipanggil dari thread latar. */
    fun sinkron(): Boolean {
        for (host in SERVER) {
            try {
                val hasil = tanya(host) ?: continue
                basisNtp = hasil.first
                basisElapsed = hasil.second
                selisihJamTv = hasil.first - (System.currentTimeMillis() - (SystemClock.elapsedRealtime() - hasil.second))
                ntpOk = true
                sumber = host
                terakhirSinkron = hasil.first
                return true
            } catch (e: Exception) {
                Log.w("MasjidKu", "NTP $host gagal: ${e.message}")
            }
        }
        return false
    }

    private fun tanya(host: String): Pair<Long, Long>? {
        val soket = DatagramSocket()
        try {
            soket.soTimeout = 4000
            val alamat = InetAddress.getByName(host)
            val buf = ByteArray(48)
            buf[0] = 0x1B // LI 0, versi 3, mode klien
            val kirimWall = System.currentTimeMillis()
            val kirimElapsed = SystemClock.elapsedRealtime()
            tulisWaktu(buf, 40, kirimWall)
            soket.send(DatagramPacket(buf, buf.size, alamat, 123))
            val balas = DatagramPacket(buf, buf.size)
            soket.receive(balas)
            val terimaElapsed = SystemClock.elapsedRealtime()
            val terimaWall = kirimWall + (terimaElapsed - kirimElapsed)

            val mode = buf[0].toInt() and 0x7
            val stratum = buf[1].toInt() and 0xff
            if (mode != 4 && mode != 5) return null
            if (stratum == 0 || stratum > 15) return null

            val asal = bacaWaktu(buf, 24)
            val diterima = bacaWaktu(buf, 32)
            val dikirim = bacaWaktu(buf, 40)
            val selisih = ((diterima - asal) + (dikirim - terimaWall)) / 2
            val ntpSekarang = terimaWall + selisih
            // tolak hasil yang jelas salah (sebelum 2025)
            if (ntpSekarang < 1735689600000L) return null
            return Pair(ntpSekarang, terimaElapsed)
        } finally {
            soket.close()
        }
    }

    private fun bacaWaktu(b: ByteArray, off: Int): Long {
        val detik = baca32(b, off)
        val pecahan = baca32(b, off + 4)
        return (detik - NTP_EPOCH) * 1000L + (pecahan * 1000L) / 0x100000000L
    }

    private fun baca32(b: ByteArray, off: Int): Long =
        ((b[off].toLong() and 0xff) shl 24) or ((b[off + 1].toLong() and 0xff) shl 16) or
            ((b[off + 2].toLong() and 0xff) shl 8) or (b[off + 3].toLong() and 0xff)

    private fun tulisWaktu(b: ByteArray, off: Int, ms: Long) {
        val detik = ms / 1000L + NTP_EPOCH
        val pecahan = ((ms % 1000L) * 0x100000000L) / 1000L
        b[off] = (detik shr 24).toByte(); b[off + 1] = (detik shr 16).toByte()
        b[off + 2] = (detik shr 8).toByte(); b[off + 3] = detik.toByte()
        b[off + 4] = (pecahan shr 24).toByte(); b[off + 5] = (pecahan shr 16).toByte()
        b[off + 6] = (pecahan shr 8).toByte(); b[off + 7] = pecahan.toByte()
    }
}
