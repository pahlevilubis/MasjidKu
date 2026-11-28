package com.masjidku.tv.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.masjidku.tv.Media
import com.masjidku.tv.Slide
import com.masjidku.tv.Tampilan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/** Status putaran slide: slide teks/gambar berganti tiap N detik, video berganti setelah selesai diputar. */
class Putaran(
    val slides: List<Slide>,
    val langkah: Int,
    private val sinyal: MutableSharedFlow<Int>,
) {
    val jumlah: Int get() = slides.size
    fun indeks(l: Int = langkah): Int = if (slides.isEmpty()) 0 else l % slides.size
    fun slide(l: Int = langkah): Slide? = slides.getOrNull(indeks(l))
    val slideVideo: Boolean get() = slide()?.let { Media.jenis(it.media) == Media.VIDEO } == true
    fun selesai(l: Int) {
        sinyal.tryEmit(l)
    }
}

@Composable
fun rememberPutaran(slides: List<Slide>, detik: Int): Putaran {
    var langkah by remember { mutableIntStateOf(0) }
    val sinyal = remember { MutableSharedFlow<Int>(replay = 1, extraBufferCapacity = 8) }
    val p = Putaran(slides, langkah, sinyal)
    val sekarang = p.slide()
    val video = p.slideVideo
    LaunchedEffect(langkah, slides.size, sekarang) {
        if (video) {
            // tunggu video selesai (paling lama 30 menit, jaga-jaga bila video macet)
            withTimeoutOrNull(30 * 60_000L) { sinyal.first { it == langkah } }
        } else {
            delay(detik.coerceAtLeast(4) * 1000L)
        }
        langkah += 1
    }
    return p
}

/** Pemutar video berkas lokal. Bila gagal diputar, slide langsung dilewati. */
@Composable
fun PemutarVideo(berkas: File, suara: Boolean, volume: Int, modifier: Modifier, onSelesai: () -> Unit) {
    val selesai by rememberUpdatedState(onSelesai)
    AndroidView(
        modifier = modifier.background(Color.Black),
        factory = { c ->
            val wadah = FrameLayout(c)
            val v = VideoView(c)
            wadah.addView(
                v,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
            )
            v.setOnPreparedListener { mp ->
                val vol = if (suara) volume.coerceIn(0, 100) / 100f else 0f
                try {
                    mp.setVolume(vol, vol)
                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                } catch (_: Exception) {
                }
                v.start()
            }
            v.setOnCompletionListener { selesai() }
            v.setOnErrorListener { _, _, _ ->
                selesai()
                true
            }
            v.setVideoPath(berkas.absolutePath)
            wadah
        },
        onRelease = { wadah ->
            (wadah.getChildAt(0) as? VideoView)?.stopPlayback()
        }
    )
}

/** Gambar dari berkas, diperkecil saat dimuat agar hemat memori TV. */
@Composable
fun GambarBerkas(berkas: File, modifier: Modifier) {
    val bmp by produceState<ImageBitmap?>(null, berkas.path) {
        value = withContext(Dispatchers.IO) { muatBitmap(berkas, 1600)?.asImageBitmap() }
    }
    val b = bmp
    if (b != null) {
        Image(b, contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
    }
}

private fun muatBitmap(f: File, maks: Int): Bitmap? = try {
    val ukur = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(f.absolutePath, ukur)
    var skala = 1
    while (ukur.outWidth / skala > maks || ukur.outHeight / skala > maks) skala *= 2
    BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = skala })
} catch (e: Throwable) {
    null
}

/** Video layar penuh, dengan jam dan hitung mundur kecil di pojok bawah. */
@Composable
fun LayarVideoPenuh(t: Tampilan, p: Palet, sk: Skala, putaran: Putaran) {
    val l = putaran.langkah
    val s = putaran.slide(l) ?: return
    val f = Media.berkas(s.media) ?: return
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        androidx.compose.runtime.key(l) {
            PemutarVideo(f, t.videoSuara, t.volumeVideo, Modifier.fillMaxSize()) { putaran.selesai(l) }
        }
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(sk.dp(18f))
                .clip(RoundedCornerShape(sk.dp(12f)))
                .background(p.latarBawah.copy(alpha = 0.82f))
                .padding(horizontal = sk.dp(18f), vertical = sk.dp(8f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${t.jam}:${t.detik}", color = p.teks, fontSize = sk.sp(30f),
                fontWeight = FontWeight.Bold, style = ANGKA
            )
            Spacer(Modifier.width(sk.dp(16f)))
            Text("Menuju ${t.menuju}", color = p.teksRedup, fontSize = sk.sp(20f))
            Spacer(Modifier.width(sk.dp(10f)))
            Text(
                t.hitungMundur, color = p.aksen, fontSize = sk.sp(28f),
                fontWeight = FontWeight.Bold, style = ANGKA
            )
        }
    }
}
