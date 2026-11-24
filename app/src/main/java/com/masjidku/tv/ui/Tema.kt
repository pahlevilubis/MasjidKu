package com.masjidku.tv.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

data class Palet(
    val latarAtas: Color,
    val latarBawah: Color,
    val panel: Color,
    val aksen: Color,
    val teks: Color,
    val teksRedup: Color,
    val teksDiAksen: Color,
    val peringatan: Color,
)

val PALET = listOf(
    // 0 Hijau Zamrud
    Palet(Color(0xFF0A3526), Color(0xFF041B13), Color(0xCC0F4331), Color(0xFFE9C46A),
        Color(0xFFF7F3E8), Color(0xFFA9C7B8), Color(0xFF1E1606), Color(0xFFE76F51)),
    // 1 Biru Malam
    Palet(Color(0xFF0D2341), Color(0xFF050D1A), Color(0xCC14305A), Color(0xFF5CC8F2),
        Color(0xFFF0F6FF), Color(0xFFA2B6CC), Color(0xFF03111D), Color(0xFFFF7A59)),
    // 2 Coklat Klasik
    Palet(Color(0xFF3A2416), Color(0xFF170D07), Color(0xCC4A2E1C), Color(0xFFE3B65A),
        Color(0xFFFCF4E6), Color(0xFFCDB79D), Color(0xFF221409), Color(0xFFE76F51)),
    // 3 Hitam Kontras
    Palet(Color(0xFF111111), Color(0xFF000000), Color(0xFF1B1B1B), Color(0xFFFFD60A),
        Color(0xFFFFFFFF), Color(0xFFBDBDBD), Color(0xFF000000), Color(0xFFFF5A36)),
)

fun palet(i: Int): Palet = PALET[i.coerceIn(0, PALET.size - 1)]

/** Ukuran relatif terhadap layar: 1 satuan = 1/1000 lebar layar 16:9. */
class Skala(lebar: Dp, tinggi: Dp, private val density: Density) {
    val u: Dp = minOf(lebar / 1000f, tinggi / 562.5f)
    fun dp(x: Float): Dp = u * x
    fun sp(x: Float): TextUnit = with(density) { (u * x).toSp() }
}

/** Angka dengan lebar sama, supaya jam tidak "bergoyang". */
val ANGKA = TextStyle(fontFeatureSettings = "tnum")
