package com.masjidku.tv

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.masjidku.tv.inti.Fase
import com.masjidku.tv.inti.Hisab
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Method
import fi.iki.elonen.NanoHTTPD.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Panel pengaturan lewat HP: http://<ip-tv>:8080
 * Semua API (kecuali /api/info & /api/masuk) wajib menyertakan header X-Pin.
 */
class Panel(private val ctx: Context) : NanoHTTPD(PORT) {

    companion object {
        const val PORT = 8080
        private const val MAKS_AUDIO = 25L * 1024 * 1024

        fun alamatIp(): List<String> = try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .mapNotNull { it.hostAddress }
                .filter { it.isNotEmpty() }
                .distinct()
        } catch (e: Exception) {
            emptyList()
        }

        fun alamatPanel(): List<String> = alamatIp().map { "http://$it:$PORT" }

        private fun aliasBeranda(ctx: Context) = ComponentName(ctx.packageName, "com.masjidku.tv.Beranda")

        fun launcherAktif(ctx: Context): Boolean = try {
            ctx.packageManager.getComponentEnabledSetting(aliasBeranda(ctx)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } catch (e: Exception) {
            false
        }

        fun aturLauncher(ctx: Context, aktif: Boolean) {
            ctx.packageManager.setComponentEnabledSetting(
                aliasBeranda(ctx),
                if (aktif) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        /** null = tidak perlu (Android 5), true/false = status izin tampil di atas aplikasi lain */
        fun izinOverlay(ctx: Context): Boolean? =
            if (Build.VERSION.SDK_INT >= 23) Settings.canDrawOverlays(ctx) else null
    }

    override fun serve(s: IHTTPSession): Response = try {
        rute(s)
    } catch (e: Exception) {
        Log.e("MasjidKu", "Panel error", e)
        json(Response.Status.INTERNAL_ERROR, JSONObject().put("ok", false).put("pesan", e.message ?: "Terjadi kesalahan"))
    }

    private fun rute(s: IHTTPSession): Response {
        val uri = s.uri ?: "/"
        val post = s.method == Method.POST
        val unggah = post && ((uri.startsWith("/api/audio/") && !uri.endsWith("/hapus")) || uri == "/api/media")
        // badan permintaan selalu dibaca habis (kecuali unggah berkas) agar koneksi tetap rapi
        val badan = if (post && !unggah) bacaBadan(s) else ""

        // berkas video/gambar untuk pratinjau di panel (PIN lewat ?pin= karena dipakai tag <img>/<video>)
        if (uri.startsWith("/media/")) {
            if (s.parameters["pin"]?.firstOrNull() != Repo.cfg.value.pin) {
                return json(Response.Status.UNAUTHORIZED, JSONObject().put("ok", false).put("pesan", "PIN diperlukan"))
            }
            val f = Media.berkas(uri.removePrefix("/media/"))
                ?: return json(Response.Status.NOT_FOUND, JSONObject().put("ok", false).put("pesan", "Tidak ditemukan"))
            val r = NanoHTTPD.newFixedLengthResponse(Response.Status.OK, Media.mime(f.name), f.inputStream(), f.length())
            r.addHeader("Cache-Control", "max-age=3600")
            return r
        }

        if (!uri.startsWith("/api/")) return halaman()

        if (uri == "/api/info") {
            return json(Response.Status.OK, JSONObject()
                .put("ok", true)
                .put("nama", Repo.cfg.value.namaMasjid)
                .put("versi", BuildConfig.VERSION_NAME))
        }
        if (uri == "/api/masuk" && post) {
            val pin = JSONObject(badan.ifBlank { "{}" }).optString("pin", "")
            return if (pin == Repo.cfg.value.pin) ok() else {
                Thread.sleep(800)
                json(Response.Status.FORBIDDEN, JSONObject().put("ok", false).put("pesan", "PIN salah"))
            }
        }

        if (s.headers["x-pin"] != Repo.cfg.value.pin) {
            return json(Response.Status.UNAUTHORIZED, JSONObject().put("ok", false).put("pesan", "PIN diperlukan"))
        }

        return when {
            uri == "/api/status" -> json(Response.Status.OK, status())
            uri == "/api/config" && !post -> json(Response.Status.OK, Repo.cfg.value.keJson())
            uri == "/api/config" && post -> simpanConfig(badan)
            uri.startsWith("/api/audio/") && post -> audio(s, uri.removePrefix("/api/audio/"))
            uri == "/api/media" && !post -> json(Response.Status.OK, daftarMedia())
            uri == "/api/media" && post -> unggahMedia(s)
            uri.startsWith("/api/media/") && uri.endsWith("/hapus") && post -> hapusMedia(uri)
            uri == "/api/tes" && post -> tes(badan)
            uri == "/api/pratinjau" && post -> pratinjau(badan)
            uri == "/api/muat-ulang" && post -> {
                Mesin.perintah.tryEmit("muat-ulang")
                Mesin.bawaKeDepan(paksa = true)
                ok()
            }
            uri == "/api/launcher" && post -> {
                val aktif = JSONObject(badan.ifBlank { "{}" }).optBoolean("aktif", false)
                aturLauncher(ctx, aktif)
                ok()
            }
            uri == "/api/pembaruan/cek" && post -> {
                Pembaru.cekSekarang()
                ok()
            }
            uri == "/api/pembaruan/pasang" && post -> {
                Pembaru.perbaruiSekarang()
                ok()
            }
            uri == "/api/sinkron-jam" && post -> {
                val berhasil = Jam.sinkron()
                json(Response.Status.OK, JSONObject().put("ok", berhasil).put("sumber", Jam.sumber))
            }
            uri == "/api/log" -> {
                val f = File(Repo.dirData, "crash.txt")
                json(Response.Status.OK, JSONObject().put("ok", true).put("log", if (f.exists()) f.readText() else ""))
            }
            else -> json(Response.Status.NOT_FOUND, JSONObject().put("ok", false).put("pesan", "Tidak ditemukan"))
        }
    }

    private fun halaman(): Response {
        val html = ctx.assets.open("panel/index.html").use { String(it.readBytes(), Charsets.UTF_8) }
        val r = NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)
        r.addHeader("Cache-Control", "no-store")
        return r
    }

    private fun simpanConfig(badan: String): Response {
        val lama = Repo.cfg.value
        val baru = Config.dariJson(JSONObject(badan))
        val pin = if (baru.pin.length >= 4) baru.pin else lama.pin
        Repo.simpan(baru.copy(pin = pin, sudahDiatur = true))
        return ok()
    }

    private fun audio(s: IHTTPSession, jalur: String): Response {
        val bagian = jalur.split("/")
        val slot = bagian.getOrNull(0) ?: ""
        if (slot !in Repo.SLOT_AUDIO) return gagal("Slot suara tidak dikenal")
        if (bagian.getOrNull(1) == "hapus") {
            Repo.hapusAudio(slot)
            return ok()
        }
        val panjang = s.headers["content-length"]?.toLongOrNull() ?: 0L
        if (panjang > MAKS_AUDIO) return gagal("Berkas terlalu besar (maksimal 25 MB)")
        val berkas = HashMap<String, String>()
        s.parseBody(berkas)
        val tmp = berkas["berkas"] ?: return gagal("Berkas tidak terkirim")
        val namaAsli = s.parameters["berkas"]?.firstOrNull() ?: "suara.mp3"
        val ext = namaAsli.substringAfterLast('.', "mp3").lowercase().filter { it.isLetterOrDigit() }.take(5)
            .ifBlank { "mp3" }
        Repo.hapusAudio(slot)
        File(tmp).copyTo(File(Repo.dirAudio, "$slot.$ext"), overwrite = true)
        return ok()
    }

    private fun daftarMedia(): JSONObject {
        val dipakai = Repo.cfg.value.slides.map { it.media }.toSet()
        val arr = JSONArray()
        for (f in Media.daftar()) {
            arr.put(
                JSONObject()
                    .put("nama", f.name)
                    .put("jenis", if (Media.jenis(f.name) == Media.VIDEO) "video" else "gambar")
                    .put("ukuran", f.length())
                    .put("dipakai", f.name in dipakai)
            )
        }
        return JSONObject().put("ok", true).put("berkas", arr)
            .put("sisaRuang", Media.sisaRuang()).put("maks", Media.MAKS_BYTE)
    }

    private fun unggahMedia(s: IHTTPSession): Response {
        val panjang = s.headers["content-length"]?.toLongOrNull() ?: 0L
        if (panjang > Media.MAKS_BYTE + 1_000_000L) {
            return gagal("Berkas terlalu besar (maksimal 300 MB). Perkecil video ke 720p.")
        }
        if (Media.sisaRuang() < panjang * 2 + 150L * 1024 * 1024) {
            return gagal("Ruang penyimpanan TV tidak cukup. Hapus video lama dulu.")
        }
        val berkas = HashMap<String, String>()
        s.parseBody(berkas)
        val tmp = berkas["berkas"] ?: return gagal("Berkas tidak terkirim")
        val namaAsli = s.parameters["berkas"]?.firstOrNull() ?: "media"
        if (Media.jenis(namaAsli) == Media.TIDAK) {
            return gagal("Jenis berkas tidak didukung. Pakai MP4 untuk video, JPG atau PNG untuk gambar.")
        }
        val nama = Media.simpan(File(tmp), namaAsli)
        return json(Response.Status.OK, JSONObject().put("ok", true).put("nama", nama))
    }

    private fun hapusMedia(uri: String): Response {
        val nama = uri.removePrefix("/api/media/").removeSuffix("/hapus")
        if (!Media.hapus(nama)) return gagal("Berkas tidak ditemukan")
        val c = Repo.cfg.value
        if (c.slides.any { it.media == nama }) Repo.simpan(c.copy(slides = c.slides.filter { it.media != nama }))
        return ok()
    }

    private fun tes(badan: String): Response {
        val c = Repo.cfg.value
        when (val suara = JSONObject(badan.ifBlank { "{}" }).optString("suara", "")) {
            "beep" -> Suara.beep(3, 600, c.volumeBeep)
            "stop" -> Suara.berhenti()
            in Repo.SLOT_AUDIO -> if (!Suara.putarSlot(suara, c.volumeAdzan)) return gagal("Rekaman belum diunggah")
            else -> return gagal("Suara tidak dikenal")
        }
        return ok()
    }

    private fun pratinjau(badan: String): Response {
        val o = JSONObject(badan.ifBlank { "{}" })
        val mode = when (o.optString("mode", "")) {
            "menjelang" -> Fase.MENJELANG
            "adzan" -> Fase.ADZAN
            "iqamah" -> Fase.IQAMAH
            "sholat" -> Fase.SHOLAT
            "khutbah" -> Fase.KHUTBAH
            "selesai" -> { Mesin.hentikanPratinjau(); return ok() }
            else -> return gagal("Mode tidak dikenal")
        }
        Mesin.pratinjau(mode, o.optInt("sholat", 1), o.optInt("detik", 20).coerceIn(5, 120))
        return ok()
    }

    private fun status(): JSONObject {
        val t = Mesin.tampilan.value
        val j = Mesin.jadwal
        val nama = listOf("imsak", "subuh", "terbit", "dhuha", "dzuhur", "ashar", "maghrib", "isya")
        val jadwal = JSONObject()
        for (i in nama.indices) jadwal.put(nama[i], Hisab.jam(j[i]))
        val audio = JSONObject()
        for (slot in Repo.SLOT_AUDIO) audio.put(slot, Repo.berkasAudio(slot)?.name ?: JSONObject.NULL)
        return JSONObject()
            .put("ok", true)
            .put("versi", BuildConfig.VERSION_NAME)
            .put("jam", "${t.jam}:${t.detik}")
            .put("masehi", t.masehi)
            .put("hijriah", t.hijriah)
            .put("jadwal", jadwal)
            .put("fase", Mesin.faseTeks)
            .put("ntp", JSONObject().put("ok", Jam.ntpOk).put("sumber", Jam.sumber).put("selisihDetik", Jam.selisihJamTv / 1000))
            .put("audio", audio)
            .put("memutar", Suara.sedangMemutar)
            .put("izinOverlay", izinOverlay(ctx) ?: JSONObject.NULL)
            .put("launcher", launcherAktif(ctx))
            .put("tampil", Mesin.aktivitasTampil)
            .put("alamat", JSONArray(alamatPanel()))
            .put("android", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            .put("perangkat", "${Build.MANUFACTURER} ${Build.MODEL}")
            .put("adaLog", File(Repo.dirData, "crash.txt").exists())
            .put("pembaruan", Pembaru.statusJson())
    }

    private fun bacaBadan(s: IHTTPSession): String {
        val n = s.headers["content-length"]?.toIntOrNull() ?: 0
        if (n <= 0) return ""
        val buf = ByteArray(n)
        var off = 0
        val inp = s.inputStream
        while (off < n) {
            val r = inp.read(buf, off, n - off)
            if (r < 0) break
            off += r
        }
        return String(buf, 0, off, Charsets.UTF_8)
    }

    private fun ok() = json(Response.Status.OK, JSONObject().put("ok", true))
    private fun gagal(pesan: String) = json(Response.Status.BAD_REQUEST, JSONObject().put("ok", false).put("pesan", pesan))

    private fun json(status: Response.Status, o: JSONObject): Response {
        val r = NanoHTTPD.newFixedLengthResponse(status, "application/json; charset=utf-8", o.toString())
        r.addHeader("Cache-Control", "no-store")
        return r
    }
}
