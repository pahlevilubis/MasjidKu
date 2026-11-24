package com.masjidku.tv

import org.json.JSONArray
import org.json.JSONObject

/** Pengaturan untuk satu waktu sholat (Subuh, Dzuhur, Ashar, Maghrib, Isya). */
data class AturSholat(
    val sebelumMenit: Int,   // layar "menjelang adzan" (0 = mati)
    val sebelumSuara: Int,   // 0 diam, 1 beep, 2 rekaman tarhim
    val adzanMenit: Int,     // lama layar adzan
    val adzanSuara: Int,     // 0 diam, 1 beep, 2 rekaman adzan
    val iqamahMenit: Int,    // hitung mundur iqamah setelah layar adzan selesai
    val sholatMenit: Int,    // lama layar "sholat sedang berlangsung"
    val beepIqamah: Boolean, // bunyi beep saat hitung mundur iqamah habis
)

data class Slide(val judul: String, val isi: String)

data class Config(
    // Masjid & lokasi
    val namaMasjid: String = "Nama Masjid",
    val alamat: String = "Atur nama & alamat masjid lewat HP",
    val namaKota: String = "Jakarta",
    val lintang: Double = -6.1754,
    val bujur: Double = 106.8272,
    val zona: Int = 7,

    // Hisab
    val sudutSubuh: Double = 20.0,
    val sudutIsya: Double = 18.0,
    val ihtiyath: Int = 2,
    val mazhabAshar: Int = 1,
    val imsakMenit: Int = 10,
    val koreksi: List<Int> = List(8) { 0 }, // imsak, subuh, terbit, dhuha, dzuhur, ashar, maghrib, isya
    val koreksiHijriah: Int = 0,
    val hijriahGantiMaghrib: Boolean = true,

    // Adzan, iqamah, Jumat
    val sholat: List<AturSholat> = SHOLAT_BAWAAN,
    val jumatAktif: Boolean = true,
    val khutbahMenit: Int = 30,
    val sholatJumatMenit: Int = 15,
    val beepImsak: Boolean = false,
    val volumeAdzan: Int = 100,
    val volumeBeep: Int = 80,

    // Tampilan
    val tema: Int = 0,
    val tampilImsak: Boolean = true,
    val tampilSyuruq: Boolean = true,
    val tampilDhuha: Boolean = false,
    val tampilDetik: Boolean = true,
    val runningText: List<String> = listOf(
        "Selamat datang di rumah Allah. Mohon jaga kebersihan dan ketenangan masjid.",
        "Mohon HP disenyapkan saat sholat berjamaah.",
    ),
    val kecepatanTeks: Int = 3,
    val slides: List<Slide> = listOf(
        Slide("Selamat Datang", "Semoga Allah menerima ibadah kita semua. Luruskan dan rapatkan shaf saat sholat berjamaah."),
        Slide("Adab di Masjid", "Rapikan alas kaki, jaga kebersihan, tidak bercakap keras, dan senyapkan HP."),
    ),
    val slideDetik: Int = 12,
    val tampilPetugas: Boolean = true,
    val imam: String = "",
    val khatib: String = "",
    val muadzin: String = "",
    val tampilKas: Boolean = false,
    val kasTanggal: String = "",
    val kasMasuk: String = "",
    val kasKeluar: String = "",
    val kasSaldo: String = "",

    // Redup
    val redupAktif: Boolean = false,
    val redupMulai: String = "22:00",
    val redupSelesai: String = "03:30",
    val redupPersen: Int = 60,
    val redupSaatSholat: Boolean = true,

    // Sistem
    val autoKembaliMenit: Int = 10,
    val munculSaatAdzan: Boolean = true,
    val pakaiNtp: Boolean = true,
    val koreksiJamDetik: Int = 0,
    val pin: String = "",
    val sudahDiatur: Boolean = false,
) {
    fun keJson(): JSONObject {
        val o = JSONObject()
        o.put("namaMasjid", namaMasjid); o.put("alamat", alamat); o.put("namaKota", namaKota)
        o.put("lintang", lintang); o.put("bujur", bujur); o.put("zona", zona)
        o.put("sudutSubuh", sudutSubuh); o.put("sudutIsya", sudutIsya); o.put("ihtiyath", ihtiyath)
        o.put("mazhabAshar", mazhabAshar); o.put("imsakMenit", imsakMenit)
        o.put("koreksi", JSONArray(koreksi))
        o.put("koreksiHijriah", koreksiHijriah); o.put("hijriahGantiMaghrib", hijriahGantiMaghrib)
        val arr = JSONArray()
        for (s in sholat) {
            arr.put(JSONObject().apply {
                put("sebelumMenit", s.sebelumMenit); put("sebelumSuara", s.sebelumSuara)
                put("adzanMenit", s.adzanMenit); put("adzanSuara", s.adzanSuara)
                put("iqamahMenit", s.iqamahMenit); put("sholatMenit", s.sholatMenit)
                put("beepIqamah", s.beepIqamah)
            })
        }
        o.put("sholat", arr)
        o.put("jumatAktif", jumatAktif); o.put("khutbahMenit", khutbahMenit); o.put("sholatJumatMenit", sholatJumatMenit)
        o.put("beepImsak", beepImsak); o.put("volumeAdzan", volumeAdzan); o.put("volumeBeep", volumeBeep)
        o.put("tema", tema); o.put("tampilImsak", tampilImsak); o.put("tampilSyuruq", tampilSyuruq)
        o.put("tampilDhuha", tampilDhuha); o.put("tampilDetik", tampilDetik)
        o.put("runningText", JSONArray(runningText)); o.put("kecepatanTeks", kecepatanTeks)
        val sl = JSONArray()
        for (s in slides) sl.put(JSONObject().put("judul", s.judul).put("isi", s.isi))
        o.put("slides", sl); o.put("slideDetik", slideDetik)
        o.put("tampilPetugas", tampilPetugas); o.put("imam", imam); o.put("khatib", khatib); o.put("muadzin", muadzin)
        o.put("tampilKas", tampilKas); o.put("kasTanggal", kasTanggal); o.put("kasMasuk", kasMasuk)
        o.put("kasKeluar", kasKeluar); o.put("kasSaldo", kasSaldo)
        o.put("redupAktif", redupAktif); o.put("redupMulai", redupMulai); o.put("redupSelesai", redupSelesai)
        o.put("redupPersen", redupPersen); o.put("redupSaatSholat", redupSaatSholat)
        o.put("autoKembaliMenit", autoKembaliMenit); o.put("munculSaatAdzan", munculSaatAdzan)
        o.put("pakaiNtp", pakaiNtp); o.put("koreksiJamDetik", koreksiJamDetik)
        o.put("pin", pin); o.put("sudahDiatur", sudahDiatur)
        return o
    }

    companion object {
        val SHOLAT_BAWAAN = listOf(
            AturSholat(10, 0, 4, 1, 10, 10, true), // Subuh
            AturSholat(5, 0, 3, 1, 7, 12, true),   // Dzuhur
            AturSholat(5, 0, 3, 1, 7, 12, true),   // Ashar
            AturSholat(5, 0, 3, 1, 5, 10, true),   // Maghrib
            AturSholat(5, 0, 3, 1, 7, 12, true),   // Isya
        )

        fun dariJson(o: JSONObject): Config {
            val d = Config()
            fun s(k: String, def: String, maks: Int = 300): String =
                if (o.has(k) && !o.isNull(k)) o.optString(k, def).take(maks) else def
            fun i(k: String, def: Int, min: Int, max: Int): Int = o.optInt(k, def).coerceIn(min, max)
            fun dbl(k: String, def: Double, min: Double, max: Double): Double {
                val v = o.optDouble(k, def)
                return if (v.isNaN()) def else v.coerceIn(min, max)
            }
            fun b(k: String, def: Boolean): Boolean = o.optBoolean(k, def)

            val koreksi = o.optJSONArray("koreksi")?.let { a ->
                List(8) { idx -> a.optInt(idx, 0).coerceIn(-30, 30) }
            } ?: d.koreksi

            val sholat = o.optJSONArray("sholat")?.let { a ->
                List(5) { idx ->
                    val bawaan = SHOLAT_BAWAAN[idx]
                    val x = a.optJSONObject(idx)
                    if (x == null) bawaan else AturSholat(
                        sebelumMenit = x.optInt("sebelumMenit", bawaan.sebelumMenit).coerceIn(0, 60),
                        sebelumSuara = x.optInt("sebelumSuara", bawaan.sebelumSuara).coerceIn(0, 2),
                        adzanMenit = x.optInt("adzanMenit", bawaan.adzanMenit).coerceIn(0, 20),
                        adzanSuara = x.optInt("adzanSuara", bawaan.adzanSuara).coerceIn(0, 2),
                        iqamahMenit = x.optInt("iqamahMenit", bawaan.iqamahMenit).coerceIn(0, 60),
                        sholatMenit = x.optInt("sholatMenit", bawaan.sholatMenit).coerceIn(0, 60),
                        beepIqamah = x.optBoolean("beepIqamah", bawaan.beepIqamah),
                    )
                }
            } ?: d.sholat

            val running = o.optJSONArray("runningText")?.let { a ->
                (0 until a.length()).map { a.optString(it, "").take(500) }.filter { it.isNotBlank() }.take(30)
            } ?: d.runningText

            val slides = o.optJSONArray("slides")?.let { a ->
                (0 until a.length()).mapNotNull { idx ->
                    val x = a.optJSONObject(idx) ?: return@mapNotNull null
                    val sl = Slide(x.optString("judul", "").take(80), x.optString("isi", "").take(600))
                    if (sl.judul.isBlank() && sl.isi.isBlank()) null else sl
                }.take(20)
            } ?: d.slides

            return Config(
                namaMasjid = s("namaMasjid", d.namaMasjid, 80),
                alamat = s("alamat", d.alamat, 160),
                namaKota = s("namaKota", d.namaKota, 60),
                lintang = dbl("lintang", d.lintang, -11.5, 6.5),
                bujur = dbl("bujur", d.bujur, 94.0, 142.0),
                zona = i("zona", d.zona, 7, 9),
                sudutSubuh = dbl("sudutSubuh", d.sudutSubuh, 10.0, 25.0),
                sudutIsya = dbl("sudutIsya", d.sudutIsya, 10.0, 25.0),
                ihtiyath = i("ihtiyath", d.ihtiyath, 0, 5),
                mazhabAshar = i("mazhabAshar", d.mazhabAshar, 1, 2),
                imsakMenit = i("imsakMenit", d.imsakMenit, 0, 30),
                koreksi = koreksi,
                koreksiHijriah = i("koreksiHijriah", d.koreksiHijriah, -3, 3),
                hijriahGantiMaghrib = b("hijriahGantiMaghrib", d.hijriahGantiMaghrib),
                sholat = sholat,
                jumatAktif = b("jumatAktif", d.jumatAktif),
                khutbahMenit = i("khutbahMenit", d.khutbahMenit, 0, 90),
                sholatJumatMenit = i("sholatJumatMenit", d.sholatJumatMenit, 0, 60),
                beepImsak = b("beepImsak", d.beepImsak),
                volumeAdzan = i("volumeAdzan", d.volumeAdzan, 0, 100),
                volumeBeep = i("volumeBeep", d.volumeBeep, 0, 100),
                tema = i("tema", d.tema, 0, 3),
                tampilImsak = b("tampilImsak", d.tampilImsak),
                tampilSyuruq = b("tampilSyuruq", d.tampilSyuruq),
                tampilDhuha = b("tampilDhuha", d.tampilDhuha),
                tampilDetik = b("tampilDetik", d.tampilDetik),
                runningText = running,
                kecepatanTeks = i("kecepatanTeks", d.kecepatanTeks, 1, 5),
                slides = slides,
                slideDetik = i("slideDetik", d.slideDetik, 4, 120),
                tampilPetugas = b("tampilPetugas", d.tampilPetugas),
                imam = s("imam", d.imam, 80),
                khatib = s("khatib", d.khatib, 80),
                muadzin = s("muadzin", d.muadzin, 80),
                tampilKas = b("tampilKas", d.tampilKas),
                kasTanggal = s("kasTanggal", d.kasTanggal, 60),
                kasMasuk = s("kasMasuk", d.kasMasuk, 60),
                kasKeluar = s("kasKeluar", d.kasKeluar, 60),
                kasSaldo = s("kasSaldo", d.kasSaldo, 60),
                redupAktif = b("redupAktif", d.redupAktif),
                redupMulai = jamValid(s("redupMulai", d.redupMulai, 5), d.redupMulai),
                redupSelesai = jamValid(s("redupSelesai", d.redupSelesai, 5), d.redupSelesai),
                redupPersen = i("redupPersen", d.redupPersen, 0, 90),
                redupSaatSholat = b("redupSaatSholat", d.redupSaatSholat),
                autoKembaliMenit = i("autoKembaliMenit", d.autoKembaliMenit, 0, 240),
                munculSaatAdzan = b("munculSaatAdzan", d.munculSaatAdzan),
                pakaiNtp = b("pakaiNtp", d.pakaiNtp),
                koreksiJamDetik = i("koreksiJamDetik", d.koreksiJamDetik, -3600, 3600),
                pin = s("pin", d.pin, 12).filter { it.isDigit() },
                sudahDiatur = b("sudahDiatur", d.sudahDiatur),
            )
        }

        /** "HH:MM" -> menit sejak 00:00, atau null bila tidak valid. */
        fun menitDari(jam: String): Int? {
            val p = jam.trim().split(":")
            if (p.size != 2) return null
            val h = p[0].toIntOrNull() ?: return null
            val m = p[1].toIntOrNull() ?: return null
            if (h !in 0..23 || m !in 0..59) return null
            return h * 60 + m
        }

        private fun jamValid(jam: String, def: String): String = if (menitDari(jam) != null) jam else def
    }
}
