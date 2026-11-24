package com.masjidku.tv

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.masjidku.tv.inti.Fase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Pembaruan aplikasi lewat internet.
 * - Memeriksa versi.json di rilis terbaru (setiap 6 jam bila otomatis aktif).
 * - Mengunduh APK, memeriksa ukuran, SHA-256, dan sertifikat tanda tangan.
 * - Memasang lewat PackageInstaller: tanpa konfirmasi di Android 12+ bila MasjidKu
 *   sendiri yang memasang versi sebelumnya; selain itu menunggu pengurus menekan Pasang di TV.
 * - Pemasangan otomatis hanya dilakukan saat layar normal dan sholat berikutnya > 30 menit lagi.
 */
object Pembaru {
    data class Info(
        val kode: Int,
        val nama: String,
        val apk: String,
        val ukuran: Long,
        val sha256: String,
        val sertifikat: String,
        val catatan: String,
    )

    const val BELUM = "belum"
    const val MEMERIKSA = "memeriksa"
    const val TERBARU = "terbaru"
    const val TERSEDIA = "tersedia"
    const val MENGUNDUH = "mengunduh"
    const val SIAP = "siap"
    const val MEMASANG = "memasang"
    const val KONFIRMASI = "konfirmasi"
    const val MANUAL = "manual"
    const val GAGAL = "gagal"

    private const val AKSI_HASIL = "com.masjidku.tv.HASIL_PASANG"
    private const val JEDA_CEK = 6 * 3_600_000L

    @Volatile var status = BELUM
        private set
    @Volatile var pesan = ""
        private set
    @Volatile var progres = 0
        private set
    @Volatile var info: Info? = null
        private set
    @Volatile var perluKonfirmasi = false
        private set

    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val kunci = Mutex()
    private var terakhirCek = -JEDA_CEK
    private var berkas: File? = null

    private val dirUnduh: File get() = File(app.filesDir, "pembaruan").apply { mkdirs() }

    fun mulai(ctx: Context) {
        app = ctx.applicationContext
        bersihkan()
        scope.launch {
            delay(3 * 60_000L)
            while (isActive) {
                try {
                    if (Repo.cfg.value.updateOtomatis) otomatis()
                } catch (e: Exception) {
                    Log.w("MasjidKu", "Pembaruan otomatis gagal: ${e.message}")
                }
                delay(5 * 60_000L)
            }
        }
    }

    /** Dari panel HP: periksa sekarang. */
    fun cekSekarang() {
        scope.launch { kunci.withLock { cek() } }
    }

    /** Dari panel HP atau remote: periksa, unduh bila perlu, lalu pasang (dengan konfirmasi di TV bila diminta). */
    fun perbaruiSekarang() {
        scope.launch {
            kunci.withLock {
                if (status == MEMASANG || status == MENGUNDUH) return@withLock
                if (info == null || status == BELUM || status == GAGAL || status == TERBARU) cek()
                if (status != TERSEDIA && status != SIAP && status != KONFIRMASI) return@withLock
                if (!siapPasang()) return@withLock
                Mesin.bawaKeDepan(paksa = true)
                delay(1500)
                pasang(otomatis = false)
            }
        }
    }

    private suspend fun otomatis() {
        kunci.withLock {
            val sekarang = SystemClock.elapsedRealtime()
            if (sekarang - terakhirCek >= JEDA_CEK && status != SIAP && status != MEMASANG) cek()
            if (status == TERSEDIA) siapPasang()
            if (status == SIAP && !perluKonfirmasi && waktunyaAman()) pasang(otomatis = true)
        }
    }

    private fun waktunyaAman(): Boolean {
        val f = Mesin.faseAsli ?: return false
        return f.mode == Fase.NORMAL && f.detikMenujuBerikut > 30 * 60
    }

    // ---------- periksa ----------

    private fun cek() {
        status = MEMERIKSA
        pesan = ""
        terakhirCek = SystemClock.elapsedRealtime()
        try {
            val teks = ambilTeks(BuildConfig.URL_PEMBARUAN)
            val o = JSONObject(teks)
            val baru = Info(
                kode = o.getInt("versionCode"),
                nama = o.optString("versionName", ""),
                apk = o.getString("apk"),
                ukuran = o.optLong("ukuran", 0L),
                sha256 = o.optString("sha256", "").lowercase(),
                sertifikat = o.optString("sertifikat", "").lowercase().replace(":", ""),
                catatan = o.optString("catatan", "").take(300),
            )
            info = baru
            if (baru.kode <= BuildConfig.VERSION_CODE) {
                status = TERBARU
                return
            }
            val milikku = sertifikatSendiri()
            if (baru.sertifikat.isNotEmpty() && milikku.isNotEmpty() && baru.sertifikat != milikku) {
                status = MANUAL
                pesan = "Versi ${baru.nama} memakai tanda tangan berbeda, jadi harus dipasang manual (cadangkan pengaturan, copot, pasang ulang)."
                return
            }
            val ada = File(dirUnduh, "MasjidKu-${baru.kode}.apk")
            if (ada.exists() && ada.length() == baru.ukuran && (baru.sha256.isEmpty() || sha256(ada) == baru.sha256)) {
                berkas = ada
                status = SIAP
            } else {
                status = TERSEDIA
            }
        } catch (e: Exception) {
            status = GAGAL
            pesan = "Tidak bisa memeriksa pembaruan: ${e.message ?: "tidak ada internet"}"
        }
    }

    // ---------- unduh ----------

    private fun siapPasang(): Boolean {
        if (status == SIAP || status == KONFIRMASI) {
            val f = berkas
            if (f != null && f.exists()) return true
        }
        val i = info ?: return false
        status = MENGUNDUH
        progres = 0
        val tujuan = File(dirUnduh, "MasjidKu-${i.kode}.apk")
        val sementara = File(dirUnduh, "unduhan.tmp")
        try {
            val conn = buka(i.apk)
            val total = if (i.ukuran > 0) i.ukuran else conn.contentLength.toLong()
            conn.inputStream.use { inp ->
                sementara.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var jumlah = 0L
                    while (true) {
                        val n = inp.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        jumlah += n
                        if (total > 0) progres = ((jumlah * 100) / total).toInt().coerceIn(0, 100)
                    }
                }
            }
            conn.disconnect()
            if (i.ukuran > 0 && sementara.length() != i.ukuran) throw IllegalStateException("ukuran berkas tidak cocok")
            if (i.sha256.isNotEmpty() && sha256(sementara) != i.sha256) throw IllegalStateException("berkas rusak (SHA-256 tidak cocok)")
            dirUnduh.listFiles()?.filter { it.name.endsWith(".apk") }?.forEach { it.delete() }
            if (!sementara.renameTo(tujuan)) {
                sementara.copyTo(tujuan, overwrite = true)
                sementara.delete()
            }
            berkas = tujuan
            progres = 100
            status = SIAP
            perluKonfirmasi = false
            return true
        } catch (e: Exception) {
            sementara.delete()
            status = GAGAL
            pesan = "Gagal mengunduh: ${e.message ?: "koneksi terputus"}"
            return false
        }
    }

    // ---------- pasang ----------

    private fun pasang(otomatis: Boolean) {
        val f = berkas ?: return
        status = MEMASANG
        pesan = ""
        try {
            val installer = app.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            params.setAppPackageName(app.packageName)
            params.setSize(f.length())
            if (Build.VERSION.SDK_INT >= 31) {
                params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            val id = installer.createSession(params)
            val sesi = installer.openSession(id)
            try {
                sesi.openWrite("MasjidKu", 0, f.length()).use { out ->
                    f.inputStream().use { it.copyTo(out) }
                    sesi.fsync(out)
                }
                val hasil = Intent(app, HasilPasang::class.java)
                    .setAction(AKSI_HASIL)
                    .putExtra("otomatis", otomatis)
                val flag = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                val pi = PendingIntent.getBroadcast(app, id, hasil, flag)
                sesi.commit(pi.intentSender)
            } finally {
                sesi.close()
            }
        } catch (e: Exception) {
            status = GAGAL
            pesan = "Gagal memasang: ${e.message ?: "kesalahan"}"
        }
    }

    @Suppress("DEPRECATION")
    internal fun hasil(ctx: Context, intent: Intent) {
        val otomatis = intent.getBooleanExtra("otomatis", false)
        when (val st = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                if (otomatis) {
                    // tidak ada orang yang menekan tombol: tunggu pengurus
                    perluKonfirmasi = true
                    status = SIAP
                    pesan = "Siap dipasang. Perlu konfirmasi di TV."
                } else {
                    val konfirmasi: Intent? = if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    } else {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT)
                    }
                    status = KONFIRMASI
                    pesan = "Tekan Pasang / Install di layar TV."
                    try {
                        konfirmasi?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (konfirmasi != null) ctx.startActivity(konfirmasi)
                    } catch (e: Exception) {
                        status = GAGAL
                        pesan = "Layar konfirmasi tidak bisa dibuka: ${e.message}"
                    }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                status = TERBARU
                pesan = "Berhasil diperbarui."
            }
            else -> {
                status = GAGAL
                val alasan = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "kode $st"
                pesan = if (st == PackageInstaller.STATUS_FAILURE_ABORTED) "Pemasangan dibatalkan." else "Pemasangan gagal: $alasan"
                perluKonfirmasi = true
            }
        }
    }

    // ---------- status untuk panel & layar ----------

    fun teks(): String = when (status) {
        BELUM -> "Belum diperiksa"
        MEMERIKSA -> "Memeriksa…"
        TERBARU -> "Sudah versi terbaru"
        TERSEDIA -> "Versi ${info?.nama} tersedia"
        MENGUNDUH -> "Mengunduh versi ${info?.nama}… $progres%"
        SIAP -> if (perluKonfirmasi) "Versi ${info?.nama} siap dipasang (perlu konfirmasi di TV)" else "Versi ${info?.nama} siap, dipasang otomatis saat layar senggang"
        MEMASANG -> "Memasang…"
        KONFIRMASI -> "Menunggu tombol Pasang ditekan di TV"
        MANUAL -> "Versi ${info?.nama} harus dipasang manual"
        else -> "Gagal"
    }

    /** Teks kecil di layar TV bila pembaruan menunggu pengurus. */
    fun teksLayar(): String =
        if ((status == SIAP && perluKonfirmasi) || status == TERSEDIA && !Repo.cfg.value.updateOtomatis)
            "Pembaruan MasjidKu ${info?.nama} tersedia — tekan OK lalu ▼ untuk memasang"
        else ""

    fun bolehPasangDiam(): Boolean {
        if (Build.VERSION.SDK_INT < 31) return false
        return try {
            app.packageManager.getInstallSourceInfo(app.packageName).installingPackageName == app.packageName
        } catch (e: Exception) {
            false
        }
    }

    /** null = Android < 8 (tidak perlu izin terpisah) */
    fun izinPasang(): Boolean? =
        if (Build.VERSION.SDK_INT >= 26) app.packageManager.canRequestPackageInstalls() else null

    fun statusJson(): JSONObject = JSONObject()
        .put("versi", BuildConfig.VERSION_NAME)
        .put("kode", BuildConfig.VERSION_CODE)
        .put("status", status)
        .put("teks", teks())
        .put("pesan", pesan)
        .put("progres", progres)
        .put("versiBaru", info?.nama ?: "")
        .put("catatan", info?.catatan ?: "")
        .put("izinPasang", izinPasang() ?: JSONObject.NULL)
        .put("pasangDiam", bolehPasangDiam())

    // ---------- alat bantu ----------

    private fun bersihkan() {
        // hapus unduhan versi lama / yang sudah terpasang
        dirUnduh.listFiles()?.forEach { f ->
            val kode = f.name.removePrefix("MasjidKu-").removeSuffix(".apk").toIntOrNull()
            if (kode == null || kode <= BuildConfig.VERSION_CODE) f.delete()
        }
    }

    private fun buka(alamat: String): HttpURLConnection {
        var url = URL(alamat)
        repeat(6) {
            val c = url.openConnection() as HttpURLConnection
            c.connectTimeout = 15_000
            c.readTimeout = 30_000
            c.instanceFollowRedirects = false
            c.setRequestProperty("User-Agent", "MasjidKu/${BuildConfig.VERSION_NAME} Android")
            val kode = c.responseCode
            if (kode in 300..399) {
                val lokasi = c.getHeaderField("Location") ?: throw IllegalStateException("pengalihan tanpa alamat")
                c.disconnect()
                url = URL(url, lokasi)
            } else if (kode == 200) {
                return c
            } else {
                c.disconnect()
                throw IllegalStateException("server menjawab $kode")
            }
        }
        throw IllegalStateException("terlalu banyak pengalihan")
    }

    private fun ambilTeks(alamat: String): String {
        val c = buka(alamat)
        try {
            return c.inputStream.use { String(it.readBytes(), Charsets.UTF_8) }
        } finally {
            c.disconnect()
        }
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { inp ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = inp.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    @Suppress("DEPRECATION")
    private fun sertifikatSendiri(): String = try {
        val pm = app.packageManager
        val der: ByteArray? = if (Build.VERSION.SDK_INT >= 28) {
            pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
            pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNATURES).signatures?.firstOrNull()?.toByteArray()
        }
        if (der == null) "" else MessageDigest.getInstance("SHA-256").digest(der).joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        ""
    }
}

/** Menerima hasil pemasangan dari PackageInstaller. */
class HasilPasang : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Pembaru.hasil(ctx, intent)
    }
}
