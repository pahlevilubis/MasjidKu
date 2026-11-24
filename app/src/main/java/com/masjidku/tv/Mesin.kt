package com.masjidku.tv

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.masjidku.tv.inti.Fase
import com.masjidku.tv.inti.Hisab
import com.masjidku.tv.inti.Tanggal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class Kartu(val nama: String, val jam: String, val sorot: Boolean)

/** Semua yang perlu digambar layar, dihitung ulang tiap detak. */
data class Tampilan(
    val siap: Boolean = false,
    val jam: String = "--:--",
    val detik: String = "--",
    val masehi: String = "",
    val hijriah: String = "",
    val namaMasjid: String = "",
    val alamat: String = "",
    val kartu: List<Kartu> = emptyList(),
    val menuju: String = "",
    val hitungMundur: String = "",
    val mode: Int = Fase.NORMAL,
    val namaSholat: String = "",
    val jamSholat: String = "",
    val sisa: String = "",
    val progres: Float = 0f,
    val slides: List<Slide> = emptyList(),
    val slideDetik: Int = 12,
    val runningText: String = "",
    val kecepatan: Int = 3,
    val tema: Int = 0,
    val redup: Float = 0f,
    val redupSholat: Boolean = true,
    val tampilDetik: Boolean = true,
    val peringatanJam: Boolean = false,
    val belumDiatur: Boolean = true,
    val pratinjau: Boolean = false,
    val pembaruan: String = "",
)

/** Mesin utama: menghitung jadwal, menentukan layar, membunyikan adzan/beep. */
object Mesin {
    val tampilan = MutableStateFlow(Tampilan())
    val perintah = MutableSharedFlow<String>(extraBufferCapacity = 8)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var app: Context

    @Volatile var aktivitasTampil = false
    @Volatile var terakhirTampil = 0L
    private var terakhirBawa = -999_999L

    private var hariCache = Long.MIN_VALUE
    private var cfgCache: Config? = null
    @Volatile var jadwal = IntArray(Hisab.JUMLAH)
        private set
    private var subuhBesok = 0
    private var kunciFase = ""
    private var kunciImsak = ""
    @Volatile var faseTeks = ""
        private set
    /** Fase sebenarnya (bukan pratinjau), dipakai pembaruan otomatis untuk mencari waktu senggang. */
    @Volatile var faseAsli: Fase? = null
        private set

    @Volatile private var pratinjauMode = -1
    @Volatile private var pratinjauSholat = 1
    @Volatile private var pratinjauSampai = 0L
    @Volatile private var pratinjauLama = 0

    fun mulai(ctx: Context) {
        app = ctx.applicationContext
        terakhirTampil = SystemClock.elapsedRealtime()
        scope.launch {
            while (isActive) {
                try {
                    detak()
                } catch (e: Exception) {
                    Log.e("MasjidKu", "Detak gagal", e)
                }
                delay(250)
            }
        }
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                val ok = Repo.cfg.value.pakaiNtp && Jam.sinkron()
                delay(if (ok) 6 * 3_600_000L else 5 * 60_000L)
            }
        }
    }

    /** Setelah TV dinyalakan: coba tampilkan beberapa kali (launcher TV kadang menimpa). */
    fun setelahBoot() {
        scope.launch {
            for (tunda in listOf(0L, 8_000L, 20_000L, 45_000L)) {
                delay(tunda)
                if (!aktivitasTampil) buka()
            }
        }
    }

    fun pratinjau(mode: Int, sholat: Int, detik: Int) {
        pratinjauMode = mode
        pratinjauSholat = sholat.coerceIn(0, 4)
        pratinjauLama = detik
        pratinjauSampai = SystemClock.elapsedRealtime() + detik * 1000L
        bawaKeDepan(paksa = true)
    }

    fun hentikanPratinjau() {
        pratinjauSampai = 0L
    }

    fun bawaKeDepan(paksa: Boolean = false) {
        if (aktivitasTampil) return
        val t = SystemClock.elapsedRealtime()
        if (!paksa && t - terakhirBawa < 60_000L) return
        terakhirBawa = t
        buka()
    }

    private fun buka() {
        try {
            app.startActivity(Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Log.w("MasjidKu", "Tidak bisa membuka layar: ${e.message}")
        }
    }

    private fun detak() {
        val c = Repo.cfg.value
        val now = Jam.sekarang()
        val lokal = now + c.zona * 3_600_000L
        val hari = lokal.floorDiv(86_400_000L)
        val msHari = lokal.mod(86_400_000L)
        val detik = (msHari / 1000L).toInt()

        if (hari != hariCache || c != cfgCache) {
            val p = Hisab.Param().apply {
                lintang = c.lintang; bujur = c.bujur; zona = c.zona.toDouble()
                sudutSubuh = c.sudutSubuh; sudutIsya = c.sudutIsya; ihtiyath = c.ihtiyath
                mazhabAshar = c.mazhabAshar; imsakMenit = c.imsakMenit
                koreksi = c.koreksi.toIntArray()
            }
            val t = Tanggal.masehi(hari)
            jadwal = Hisab.hitung(t[0], t[1], t[2], p)
            val b = Tanggal.masehi(hari + 1)
            subuhBesok = Hisab.hitung(b[0], b[1], b[2], p)[Hisab.SUBUH]
            hariCache = hari
            cfgCache = c
        }
        val j = jadwal
        val jumat = c.jumatAktif && Tanggal.hariKe(hari) == 5

        val atur = Fase.Atur().apply {
            sebelum = IntArray(5) { c.sholat[it].sebelumMenit }
            adzan = IntArray(5) { c.sholat[it].adzanMenit }
            iqamah = IntArray(5) { c.sholat[it].iqamahMenit }
            sholat = IntArray(5) { c.sholat[it].sholatMenit }
            this.jumat = jumat
            khutbah = c.khutbahMenit
            sholatJumat = c.sholatJumatMenit
        }
        val menit5 = intArrayOf(j[Hisab.SUBUH], j[Hisab.DZUHUR], j[Hisab.ASHAR], j[Hisab.MAGHRIB], j[Hisab.ISYA])
        val asli = Fase.hitung(detik, menit5, subuhBesok, atur)
        faseAsli = asli

        // bunyi & munculkan layar hanya untuk kejadian nyata (bukan pratinjau)
        val kunci = "$hari-${asli.mode}-${asli.sholat}"
        if (kunci != kunciFase) {
            kunciFase = kunci
            pemicu(asli, c, jumat)
        }
        val imsakDetik = j[Hisab.IMSAK] * 60
        if (c.beepImsak && detik >= imsakDetik && detik < imsakDetik + 10 && kunciImsak != "$hari") {
            kunciImsak = "$hari"
            Suara.beep(3, 500, c.volumeBeep)
        }

        // pratinjau dari panel HP
        val el = SystemClock.elapsedRealtime()
        val pratinjau = el < pratinjauSampai
        val f = if (pratinjau) {
            Fase().apply {
                mode = pratinjauMode
                sholat = pratinjauSholat
                lamaDetik = pratinjauLama
                sisaDetik = ((pratinjauSampai - el) / 1000L).toInt() + 1
                berikut = asli.berikut
                detikMenujuBerikut = asli.detikMenujuBerikut
                berikutBesok = asli.berikutBesok
            }
        } else asli

        // kembali ke layar jadwal bila TV dipakai aplikasi lain terlalu lama
        if (!aktivitasTampil && c.autoKembaliMenit > 0 && el - terakhirTampil > c.autoKembaliMenit * 60_000L) {
            bawaKeDepan()
        }

        val namaDzuhur = if (jumat) "Jumat" else "Dzuhur"
        fun namaSholat(i: Int) = if (i == 1) namaDzuhur else Fase.NAMA_SHOLAT[i]

        val sorot = if (f.mode == Fase.NORMAL) f.berikut else f.sholat
        val kartu = ArrayList<Kartu>()
        if (c.tampilImsak) kartu.add(Kartu("Imsak", Hisab.jam(j[Hisab.IMSAK]), false))
        kartu.add(Kartu("Subuh", Hisab.jam(j[Hisab.SUBUH]), sorot == 0))
        if (c.tampilSyuruq) kartu.add(Kartu("Syuruq", Hisab.jam(j[Hisab.TERBIT]), false))
        if (c.tampilDhuha) kartu.add(Kartu("Dhuha", Hisab.jam(j[Hisab.DHUHA]), false))
        kartu.add(Kartu(namaDzuhur, Hisab.jam(j[Hisab.DZUHUR]), sorot == 1))
        kartu.add(Kartu("Ashar", Hisab.jam(j[Hisab.ASHAR]), sorot == 2))
        kartu.add(Kartu("Maghrib", Hisab.jam(j[Hisab.MAGHRIB]), sorot == 3))
        kartu.add(Kartu("Isya", Hisab.jam(j[Hisab.ISYA]), sorot == 4))

        val hariHijriah = hari + c.koreksiHijriah +
            (if (c.hijriahGantiMaghrib && detik >= j[Hisab.MAGHRIB] * 60) 1 else 0)

        val slides = ArrayList<Slide>()
        if (c.tampilPetugas && (c.khatib.isNotBlank() || c.imam.isNotBlank() || c.muadzin.isNotBlank())) {
            val isi = buildList {
                if (c.khatib.isNotBlank()) add("Khatib : ${c.khatib}")
                if (c.imam.isNotBlank()) add("Imam : ${c.imam}")
                if (c.muadzin.isNotBlank()) add("Muadzin : ${c.muadzin}")
            }.joinToString("\n")
            slides.add(Slide("Petugas Jumat", isi))
        }
        if (c.tampilKas && (c.kasSaldo.isNotBlank() || c.kasMasuk.isNotBlank() || c.kasKeluar.isNotBlank())) {
            val isi = buildList {
                if (c.kasMasuk.isNotBlank()) add("Pemasukan : ${c.kasMasuk}")
                if (c.kasKeluar.isNotBlank()) add("Pengeluaran : ${c.kasKeluar}")
                if (c.kasSaldo.isNotBlank()) add("Saldo : ${c.kasSaldo}")
            }.joinToString("\n")
            val judul = if (c.kasTanggal.isNotBlank()) "Kas Masjid (${c.kasTanggal})" else "Kas Masjid"
            slides.add(Slide(judul, isi))
        }
        slides.addAll(c.slides)
        if (slides.isEmpty()) {
            slides.add(Slide("Jadwal Hari Ini", "Imsak ${Hisab.jam(j[0])}  •  Terbit ${Hisab.jam(j[2])}  •  Dhuha ${Hisab.jam(j[3])}"))
        }

        val menitSekarang = detik / 60
        val redupMalam = if (c.redupAktif) {
            val a = Config.menitDari(c.redupMulai) ?: 0
            val b = Config.menitDari(c.redupSelesai) ?: 0
            val masuk = if (a <= b) menitSekarang in a until b else (menitSekarang >= a || menitSekarang < b)
            if (masuk) c.redupPersen / 100f * 0.9f else 0f
        } else 0f

        val tahun = Tanggal.masehi(hari)[0]
        val sholatAktif = if (f.sholat in 0..4) f.sholat else f.berikut
        val jamSholatAktif = Hisab.jam(menit5[sholatAktif])
        val nama = namaSholat(sholatAktif)

        faseTeks = when (asli.mode) {
            Fase.MENJELANG -> "Menjelang adzan ${namaSholat(asli.sholat)} (${Fase.hitungMundur(asli.sisaDetik)})"
            Fase.ADZAN -> "Adzan ${namaSholat(asli.sholat)}"
            Fase.IQAMAH -> "Menunggu iqamah ${namaSholat(asli.sholat)} (${Fase.hitungMundur(asli.sisaDetik)})"
            Fase.SHOLAT -> "Sholat ${namaSholat(asli.sholat)} berlangsung"
            Fase.KHUTBAH -> "Khutbah Jumat"
            else -> "Normal, menuju ${namaSholat(asli.berikut)} (${Fase.hitungMundur(asli.detikMenujuBerikut)})"
        }

        val jamTeks = (detik / 3600).let { if (it < 10) "0$it" else "$it" } + ":" +
            ((detik % 3600) / 60).let { if (it < 10) "0$it" else "$it" }
        val detikTeks = (detik % 60).let { if (it < 10) "0$it" else "$it" }

        tampilan.value = Tampilan(
            siap = true,
            jam = jamTeks,
            detik = detikTeks,
            masehi = Tanggal.teksMasehi(hari),
            hijriah = Tanggal.teksHijriah(hariHijriah),
            namaMasjid = c.namaMasjid,
            alamat = c.alamat,
            kartu = kartu,
            menuju = namaSholat(f.berikut),
            hitungMundur = Fase.hitungMundur(f.detikMenujuBerikut),
            mode = f.mode,
            namaSholat = nama,
            jamSholat = jamSholatAktif,
            sisa = Fase.hitungMundur(f.sisaDetik),
            progres = if (f.lamaDetik > 0) 1f - f.sisaDetik.toFloat() / f.lamaDetik else 0f,
            slides = slides,
            slideDetik = c.slideDetik,
            runningText = c.runningText.filter { it.isNotBlank() }.joinToString("        ✦        "),
            kecepatan = c.kecepatanTeks,
            tema = c.tema,
            redup = redupMalam,
            redupSholat = c.redupSaatSholat,
            tampilDetik = c.tampilDetik,
            peringatanJam = !Jam.ntpOk && tahun < 2025,
            belumDiatur = !c.sudahDiatur,
            pratinjau = pratinjau,
            pembaruan = Pembaru.teksLayar(),
        )
    }

    private fun pemicu(f: Fase, c: Config, jumat: Boolean) {
        if (f.sholat !in 0..4) return
        val a = c.sholat[f.sholat]
        when (f.mode) {
            Fase.MENJELANG -> if (f.berlalu() <= 15) {
                when (a.sebelumSuara) {
                    1 -> Suara.beep(2, 400, c.volumeBeep)
                    2 -> if (!Suara.putarSlot("tarhim", c.volumeAdzan)) Suara.beep(2, 400, c.volumeBeep)
                }
                if (c.munculSaatAdzan) bawaKeDepan(paksa = true)
            }
            Fase.ADZAN -> if (f.berlalu() <= 20) {
                when (a.adzanSuara) {
                    1 -> Suara.beep(3, 800, c.volumeBeep)
                    2 -> {
                        val slot = if (f.sholat == 0 && Repo.berkasAudio("adzan_subuh") != null) "adzan_subuh" else "adzan_umum"
                        if (!Suara.putarSlot(slot, c.volumeAdzan)) Suara.beep(3, 800, c.volumeBeep)
                    }
                }
                if (c.munculSaatAdzan) bawaKeDepan(paksa = true)
            }
            Fase.SHOLAT -> if (f.berlalu() <= 5 && a.beepIqamah && !(jumat && f.sholat == 1)) {
                Suara.beep(1, 1500, c.volumeBeep)
            }
            else -> {}
        }
    }
}
