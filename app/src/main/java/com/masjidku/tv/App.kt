package com.masjidku.tv

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class App : Application() {

    private var panel: Panel? = null

    override fun onCreate() {
        super.onCreate()
        pasangPenjagaCrash()
        Repo.muat(this)
        Mesin.mulai(this)
        mulaiPanel()

        // TV yang bangun dari standby tidak mengirim BOOT_COMPLETED: tampilkan lagi saat layar menyala
        ContextCompat.registerReceiver(
            this,
            object : BroadcastReceiver() {
                override fun onReceive(c: Context, i: Intent) {
                    Mesin.setelahBoot()
                }
            },
            IntentFilter(Intent.ACTION_SCREEN_ON),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun mulaiPanel() {
        try {
            val p = Panel(this)
            p.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            panel = p
        } catch (e: Exception) {
            Log.e("MasjidKu", "Panel gagal dimulai", e)
        }
    }

    private fun pasangPenjagaCrash() {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            try {
                val f = File(filesDir, "crash.txt")
                val barusan = f.exists() && System.currentTimeMillis() - f.lastModified() < 60_000L
                val waktu = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                f.writeText("$waktu\n${Log.getStackTraceString(e)}")
                // mulai ulang otomatis, kecuali baru saja crash (hindari putaran tanpa henti)
                if (!barusan) {
                    val i = Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    val pi = PendingIntent.getActivity(
                        this, 7, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                    )
                    val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    am.set(AlarmManager.RTC, System.currentTimeMillis() + 3000L, pi)
                }
            } catch (_: Throwable) {
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }
}
