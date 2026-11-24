package com.masjidku.tv

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.masjidku.tv.ui.InfoSetup
import com.masjidku.tv.ui.LayarUtama

class MainActivity : ComponentActivity() {

    private var info by mutableStateOf<InfoSetup?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        sembunyikanBar()
        setContent {
            val t by Mesin.tampilan.collectAsState()
            LaunchedEffect(Unit) {
                Mesin.perintah.collect { p ->
                    if (p == "muat-ulang") recreate()
                }
            }
            LayarUtama(t, info)
        }
    }

    override fun onResume() {
        super.onResume()
        Mesin.aktivitasTampil = true
        Mesin.terakhirTampil = SystemClock.elapsedRealtime()
        sembunyikanBar()
    }

    override fun onPause() {
        super.onPause()
        Mesin.aktivitasTampil = false
        Mesin.terakhirTampil = SystemClock.elapsedRealtime()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) sembunyikanBar()
    }

    private fun sembunyikanBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val c = WindowCompat.getInsetsController(window, window.decorView)
        c.hide(WindowInsetsCompat.Type.systemBars())
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun bukaTutupInfo() {
        info = if (info != null) null else InfoSetup(
            alamat = Panel.alamatPanel(),
            pin = Repo.cfg.value.pin,
            jam = if (Jam.ntpOk) "Sinkron dari internet (${Jam.sumber})" else "Memakai jam TV (belum tersinkron internet)",
            izinOverlay = Panel.izinOverlay(this),
            launcher = Panel.launcherAktif(this),
            versi = BuildConfig.VERSION_NAME,
            pembaruan = Pembaru.teks(),
            adaPembaruan = Pembaru.status in setOf(Pembaru.TERSEDIA, Pembaru.SIAP, Pembaru.KONFIRMASI),
            perangkat = "${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE}",
        )
    }

    private fun bukaIzin() {
        if (Build.VERSION.SDK_INT < 23) {
            Toast.makeText(this, "Android ini tidak memerlukan izin tambahan", Toast.LENGTH_LONG).show()
            return
        }
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Menu izin tidak tersedia di TV ini. Berikan izin lewat ADB (lihat panduan).",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO, KeyEvent.KEYCODE_SETTINGS -> {
                if (event.repeatCount == 0) bukaTutupInfo()
                return true
            }
            KeyEvent.KEYCODE_BACK -> return true
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (info != null) {
                bukaIzin()
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> if (info != null) {
                Pembaru.perbaruiSekarang()
                Toast.makeText(this, "Memeriksa dan memasang pembaruan…", Toast.LENGTH_LONG).show()
                info = null
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> if (info != null) {
                Suara.berhenti()
                Mesin.hentikanPratinjau()
                Toast.makeText(this, "Suara dihentikan", Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            bukaTutupInfo()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
