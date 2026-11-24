package com.masjidku.tv.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.masjidku.tv.Tampilan
import com.masjidku.tv.inti.Fase
import kotlinx.coroutines.delay

data class InfoSetup(
    val alamat: List<String>,
    val pin: String,
    val jam: String,
    val izinOverlay: Boolean?,
    val launcher: Boolean,
    val versi: String,
    val pembaruan: String,
    val adaPembaruan: Boolean,
    val perangkat: String,
)

@Composable
fun LayarUtama(t: Tampilan, info: InfoSetup?) {
    val p = palet(t.tema)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(p.latarAtas, p.latarBawah)))
    ) {
        val sk = Skala(maxWidth, maxHeight, LocalDensity.current)
        if (!t.siap) {
            Text(
                "Memuat jadwal…", color = p.teks, fontSize = sk.sp(30f),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            when (t.mode) {
                Fase.ADZAN -> LayarAdzan(t, p, sk)
                Fase.IQAMAH -> LayarIqamah(t, p, sk)
                Fase.SHOLAT, Fase.KHUTBAH -> LayarSholat(t, p, sk)
                else -> LayarNormal(t, p, sk)
            }
        }
        if (t.redup > 0f && t.mode != Fase.SHOLAT && t.mode != Fase.KHUTBAH) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = t.redup)))
        }
        if (t.peringatanJam) {
            Text(
                "Jam TV belum benar. Sambungkan TV ke internet atau atur jam TV.",
                color = Color.White, fontSize = sk.sp(16f), textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(p.peringatan)
                    .padding(sk.dp(6f))
            )
        }
        if (t.pratinjau) {
            Text(
                "PRATINJAU", color = p.teksDiAksen, fontSize = sk.sp(14f), fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(sk.dp(10f))
                    .clip(RoundedCornerShape(sk.dp(6f)))
                    .background(p.aksen)
                    .padding(horizontal = sk.dp(10f), vertical = sk.dp(4f))
            )
        }
        if (t.belumDiatur && info == null && t.mode == Fase.NORMAL) {
            Text(
                "Tekan OK di remote untuk cara mengatur lewat HP",
                color = p.teksDiAksen, fontSize = sk.sp(14f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = sk.dp(4f))
                    .clip(RoundedCornerShape(sk.dp(6f)))
                    .background(p.aksen.copy(alpha = 0.9f))
                    .padding(horizontal = sk.dp(12f), vertical = sk.dp(3f))
            )
        }
        if (t.pembaruan.isNotBlank() && info == null && t.mode == Fase.NORMAL) {
            Text(
                t.pembaruan, color = p.teks, fontSize = sk.sp(13f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = sk.dp(24f), bottom = sk.dp(64f))
                    .clip(RoundedCornerShape(sk.dp(6f)))
                    .background(p.latarBawah.copy(alpha = 0.85f))
                    .border(sk.dp(1f), p.aksen, RoundedCornerShape(sk.dp(6f)))
                    .padding(horizontal = sk.dp(10f), vertical = sk.dp(4f))
            )
        }
        if (info != null) LayarInfo(info, p, sk)
    }
}

@Composable
private fun LayarNormal(t: Tampilan, p: Palet, sk: Skala) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = sk.dp(22f), vertical = sk.dp(14f))
    ) {
        // Kepala: nama masjid & tanggal
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    t.namaMasjid, color = p.aksen, fontSize = sk.sp(34f), fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (t.alamat.isNotBlank()) {
                    Text(
                        t.alamat, color = p.teksRedup, fontSize = sk.sp(16f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(sk.dp(16f)))
            Column(horizontalAlignment = Alignment.End) {
                Text(t.masehi, color = p.teks, fontSize = sk.sp(21f), fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(t.hijriah, color = p.aksen, fontSize = sk.sp(18f), maxLines = 1)
            }
        }

        Spacer(Modifier.height(sk.dp(8f)))

        // Tengah: jam besar + slide info
        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                Modifier
                    .weight(1.05f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        t.jam, color = p.teks, fontSize = sk.sp(132f), fontWeight = FontWeight.Bold,
                        style = ANGKA, maxLines = 1
                    )
                    if (t.tampilDetik) {
                        Text(
                            t.detik, color = p.aksen, fontSize = sk.sp(50f), fontWeight = FontWeight.SemiBold,
                            style = ANGKA, maxLines = 1,
                            modifier = Modifier.padding(start = sk.dp(8f), bottom = sk.dp(22f))
                        )
                    }
                }
                if (t.mode == Fase.MENJELANG) {
                    KotakMenjelang(t, p, sk)
                } else {
                    KotakMenuju(t, p, sk)
                }
            }
            Spacer(Modifier.width(sk.dp(16f)))
            PanelSlide(
                t, p, sk,
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = sk.dp(6f))
            )
        }

        Spacer(Modifier.height(sk.dp(10f)))
        BarisKartu(t, p, sk)
        Spacer(Modifier.height(sk.dp(10f)))
        TeksBerjalan(t, p, sk)
    }
}

@Composable
private fun KotakMenuju(t: Tampilan, p: Palet, sk: Skala) {
    Row(
        Modifier
            .clip(RoundedCornerShape(sk.dp(14f)))
            .background(p.panel)
            .padding(horizontal = sk.dp(22f), vertical = sk.dp(10f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Menuju ${t.menuju}", color = p.teksRedup, fontSize = sk.sp(24f))
        Spacer(Modifier.width(sk.dp(16f)))
        Text(t.hitungMundur, color = p.aksen, fontSize = sk.sp(38f), fontWeight = FontWeight.Bold, style = ANGKA)
    }
}

@Composable
private fun KotakMenjelang(t: Tampilan, p: Palet, sk: Skala) {
    Column(
        Modifier
            .clip(RoundedCornerShape(sk.dp(14f)))
            .background(p.aksen)
            .padding(horizontal = sk.dp(26f), vertical = sk.dp(10f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Menjelang adzan ${t.namaSholat}", color = p.teksDiAksen,
            fontSize = sk.sp(24f), fontWeight = FontWeight.SemiBold
        )
        Text(t.sisa, color = p.teksDiAksen, fontSize = sk.sp(52f), fontWeight = FontWeight.Bold, style = ANGKA)
    }
}

@Composable
private fun PanelSlide(t: Tampilan, p: Palet, sk: Skala, modifier: Modifier) {
    val jumlah = t.slides.size
    var indeks by remember { mutableIntStateOf(0) }
    LaunchedEffect(jumlah, t.slideDetik) {
        indeks = 0
        while (true) {
            delay(t.slideDetik * 1000L)
            if (jumlah > 0) indeks = (indeks + 1) % jumlah
        }
    }
    Column(
        modifier
            .clip(RoundedCornerShape(sk.dp(18f)))
            .background(p.panel)
            .border(sk.dp(1.5f), p.aksen.copy(alpha = 0.35f), RoundedCornerShape(sk.dp(18f)))
            .padding(horizontal = sk.dp(26f), vertical = sk.dp(18f))
    ) {
        Crossfade(targetState = if (jumlah == 0) 0 else indeks % jumlah, animationSpec = tween(700), label = "slide",
            modifier = Modifier.weight(1f)) { i ->
            val s = t.slides.getOrNull(i)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                if (s != null) {
                    if (s.judul.isNotBlank()) {
                        Text(
                            s.judul, color = p.aksen, fontSize = sk.sp(28f), fontWeight = FontWeight.Bold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(sk.dp(10f)))
                    }
                    Text(
                        s.isi, color = p.teks, fontSize = sk.sp(23f), lineHeight = sk.sp(32f),
                        maxLines = 7, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (jumlah > 1) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (k in 0 until jumlah) {
                    Box(
                        Modifier
                            .padding(horizontal = sk.dp(4f))
                            .size(sk.dp(9f))
                            .clip(CircleShape)
                            .background(if (k == indeks % jumlah) p.aksen else p.teksRedup.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}

@Composable
private fun BarisKartu(t: Tampilan, p: Palet, sk: Skala) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(sk.dp(104f)),
        horizontalArrangement = Arrangement.spacedBy(sk.dp(10f))
    ) {
        for (k in t.kartu) {
            val latar = if (k.sorot) p.aksen else p.panel
            val warnaNama = if (k.sorot) p.teksDiAksen else p.teksRedup
            val warnaJam = if (k.sorot) p.teksDiAksen else p.teks
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(sk.dp(14f)))
                    .background(latar),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(k.nama, color = warnaNama, fontSize = sk.sp(22f), fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(k.jam, color = warnaJam, fontSize = sk.sp(42f), fontWeight = FontWeight.Bold, style = ANGKA, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TeksBerjalan(t: Tampilan, p: Palet, sk: Skala) {
    if (t.runningText.isBlank()) return
    Box(
        Modifier
            .fillMaxWidth()
            .height(sk.dp(46f))
            .clip(RoundedCornerShape(sk.dp(12f)))
            .background(p.panel),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            t.runningText,
            color = p.teks,
            fontSize = sk.sp(24f),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    velocity = sk.dp(25f + t.kecepatan * 18f)
                )
        )
    }
}

@Composable
private fun JamKecil(t: Tampilan, warna: Color, sk: Skala, modifier: Modifier = Modifier) {
    Text("${t.jam}:${t.detik}", color = warna, fontSize = sk.sp(30f), fontWeight = FontWeight.SemiBold,
        style = ANGKA, modifier = modifier)
}

@Composable
private fun LayarAdzan(t: Tampilan, p: Palet, sk: Skala) {
    Box(Modifier.fillMaxSize().padding(sk.dp(24f))) {
        Text(t.namaMasjid, color = p.teksRedup, fontSize = sk.sp(24f), modifier = Modifier.align(Alignment.TopStart),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        JamKecil(t, p.teks, sk, Modifier.align(Alignment.TopEnd))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ADZAN", color = p.aksen, fontSize = sk.sp(150f), fontWeight = FontWeight.Bold)
            Text(
                "Waktu sholat ${t.namaSholat} telah tiba", color = p.teks,
                fontSize = sk.sp(40f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(sk.dp(8f)))
            Text(t.jamSholat, color = p.aksen, fontSize = sk.sp(60f), fontWeight = FontWeight.Bold, style = ANGKA)
        }
        Text(
            "Mari dengarkan dan jawab seruan adzan", color = p.teksRedup, fontSize = sk.sp(24f),
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun LayarIqamah(t: Tampilan, p: Palet, sk: Skala) {
    Box(Modifier.fillMaxSize().padding(sk.dp(24f))) {
        Text(t.namaMasjid, color = p.teksRedup, fontSize = sk.sp(24f), modifier = Modifier.align(Alignment.TopStart),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        JamKecil(t, p.teks, sk, Modifier.align(Alignment.TopEnd))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("IQAMAH ${t.namaSholat.uppercase()}", color = p.teks, fontSize = sk.sp(46f), fontWeight = FontWeight.Bold)
            Text(t.sisa, color = p.aksen, fontSize = sk.sp(200f), fontWeight = FontWeight.Bold, style = ANGKA)
            Text(
                "Luruskan dan rapatkan shaf  •  Senyapkan HP", color = p.teks,
                fontSize = sk.sp(32f), textAlign = TextAlign.Center
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sk.dp(10f))
                .clip(RoundedCornerShape(sk.dp(5f)))
                .background(p.panel)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(t.progres.coerceIn(0f, 1f))
                    .background(p.aksen)
            )
        }
    }
}

@Composable
private fun LayarSholat(t: Tampilan, p: Palet, sk: Skala) {
    val redup = t.redupSholat
    val warnaUtama = if (redup) Color(0xFF6E6E6E) else p.teks
    val warnaKecil = if (redup) Color(0xFF4A4A4A) else p.teksRedup
    val latar = if (redup) Modifier.background(Color.Black) else Modifier
    val khutbah = t.mode == Fase.KHUTBAH
    Box(Modifier.fillMaxSize().then(latar).padding(sk.dp(24f))) {
        JamKecil(t, warnaKecil, sk, Modifier.align(Alignment.TopEnd))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (khutbah) "Khutbah Jumat sedang berlangsung" else "Sholat ${t.namaSholat} sedang berlangsung",
                color = warnaUtama, fontSize = sk.sp(44f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(sk.dp(12f)))
            Text(
                if (khutbah) "Mohon tidak berbicara dan senyapkan HP" else "Mohon senyapkan HP",
                color = warnaKecil, fontSize = sk.sp(28f), textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LayarInfo(info: InfoSetup, p: Palet, sk: Skala) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(sk.dp(20f)))
                .background(p.latarAtas)
                .border(sk.dp(2f), p.aksen, RoundedCornerShape(sk.dp(20f)))
                .padding(sk.dp(28f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Pengaturan MasjidKu lewat HP", color = p.aksen, fontSize = sk.sp(32f), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(sk.dp(12f)))
                Text("1. Sambungkan HP ke WiFi yang sama dengan TV ini", color = p.teks, fontSize = sk.sp(21f))
                Text("2. Buka browser di HP (Chrome), ketik alamat:", color = p.teks, fontSize = sk.sp(21f))
                if (info.alamat.isEmpty()) {
                    Text("   TV belum tersambung ke jaringan", color = p.peringatan, fontSize = sk.sp(26f), fontWeight = FontWeight.Bold)
                } else {
                    for (a in info.alamat) {
                        Text("   $a", color = p.aksen, fontSize = sk.sp(30f), fontWeight = FontWeight.Bold, style = ANGKA)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("3. Masukkan PIN:  ", color = p.teks, fontSize = sk.sp(21f))
                    Text(info.pin, color = p.aksen, fontSize = sk.sp(30f), fontWeight = FontWeight.Bold, style = ANGKA)
                }
                Spacer(Modifier.height(sk.dp(14f)))
                Text("Jam: ${info.jam}", color = p.teksRedup, fontSize = sk.sp(17f))
                val izin = when (info.izinOverlay) {
                    null -> "Tampil otomatis: siap (Android 5)"
                    true -> "Tampil otomatis saat TV nyala/adzan: AKTIF"
                    false -> "Tampil otomatis: BELUM AKTIF  (tekan ▶ untuk membuka menu izin)"
                }
                Text(izin, color = if (info.izinOverlay == false) p.peringatan else p.teksRedup, fontSize = sk.sp(17f))
                Text(
                    "Layar utama TV (launcher): " + if (info.launcher) "MasjidKu" else "bawaan TV",
                    color = p.teksRedup, fontSize = sk.sp(17f)
                )
                Text(
                    "Aplikasi versi ${info.versi}: ${info.pembaruan}" + if (info.adaPembaruan) "  (tekan ▼ untuk memasang)" else "",
                    color = if (info.adaPembaruan) p.aksen else p.teksRedup, fontSize = sk.sp(17f)
                )
                Text(info.perangkat, color = p.teksRedup, fontSize = sk.sp(15f))
                Spacer(Modifier.height(sk.dp(12f)))
                Text("Tekan OK / Kembali untuk menutup  •  ◀ hentikan suara", color = p.teks, fontSize = sk.sp(16f))
            }
            if (info.alamat.isNotEmpty()) {
                Spacer(Modifier.width(sk.dp(20f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    KodeQr(info.alamat.first(), sk.dp(220f), Modifier.clip(RoundedCornerShape(sk.dp(8f))))
                    Spacer(Modifier.height(sk.dp(8f)))
                    Text("Pindai dengan kamera HP", color = p.teksRedup, fontSize = sk.sp(15f))
                }
            }
        }
    }
}
