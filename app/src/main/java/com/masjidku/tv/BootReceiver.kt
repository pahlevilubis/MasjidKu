package com.masjidku.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** TV dinyalakan (atau aplikasi diperbarui): buka layar jadwal. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        // App.onCreate sudah berjalan (mesin & panel aktif); minta layar ditampilkan
        Mesin.setelahBoot()
    }
}
