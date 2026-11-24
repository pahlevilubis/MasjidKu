package com.masjidku.tv

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import java.io.File

/** Menyimpan & memuat pengaturan (config.json) dan berkas suara. */
object Repo {
    val SLOT_AUDIO = listOf("adzan_umum", "adzan_subuh", "tarhim")

    val cfg = MutableStateFlow(Config())
    private lateinit var berkas: File
    private lateinit var cadangan: File
    lateinit var dirAudio: File
        private set
    lateinit var dirData: File
        private set

    fun muat(ctx: Context) {
        dirData = ctx.filesDir
        berkas = File(ctx.filesDir, "config.json")
        cadangan = File(ctx.filesDir, "config.bak")
        dirAudio = File(ctx.filesDir, "audio").apply { mkdirs() }

        var c = baca(berkas) ?: baca(cadangan) ?: Config()
        if (c.pin.length < 4) {
            c = c.copy(pin = (100000..999999).random().toString())
        }
        cfg.value = c
        tulis(c)
    }

    @Synchronized
    fun simpan(c: Config) {
        tulis(c)
        cfg.value = c
    }

    private fun baca(f: File): Config? = try {
        if (f.exists()) Config.dariJson(JSONObject(f.readText(Charsets.UTF_8))) else null
    } catch (e: Exception) {
        Log.w("MasjidKu", "Gagal membaca ${f.name}", e)
        null
    }

    private fun tulis(c: Config) {
        try {
            if (berkas.exists()) berkas.copyTo(cadangan, overwrite = true)
            val tmp = File(berkas.parentFile, "config.json.tmp")
            tmp.writeText(c.keJson().toString(2), Charsets.UTF_8)
            if (!tmp.renameTo(berkas)) {
                berkas.writeText(tmp.readText(Charsets.UTF_8), Charsets.UTF_8)
                tmp.delete()
            }
        } catch (e: Exception) {
            Log.e("MasjidKu", "Gagal menyimpan pengaturan", e)
        }
    }

    fun berkasAudio(slot: String): File? =
        dirAudio.listFiles()?.firstOrNull { it.isFile && it.name.startsWith("$slot.") }

    fun hapusAudio(slot: String) {
        dirAudio.listFiles()?.filter { it.name.startsWith("$slot.") }?.forEach { it.delete() }
    }
}
