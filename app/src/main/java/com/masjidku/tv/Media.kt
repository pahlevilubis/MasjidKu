package com.masjidku.tv

import java.io.File

/** Berkas video & gambar untuk slide (disimpan di TV: files/media). */
object Media {
    const val TIDAK = 0
    const val GAMBAR = 1
    const val VIDEO = 2

    /** Batas ukuran satu berkas (TV biasanya punya ruang penyimpanan kecil). */
    const val MAKS_BYTE = 300L * 1024 * 1024

    private val EKS_GAMBAR = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    private val EKS_VIDEO = setOf("mp4", "m4v", "webm", "mkv", "3gp")

    val dir: File get() = File(Repo.dirData, "media").apply { mkdirs() }

    fun eks(nama: String) = nama.substringAfterLast('.', "").lowercase()

    fun jenis(nama: String): Int = when (eks(nama)) {
        in EKS_GAMBAR -> GAMBAR
        in EKS_VIDEO -> VIDEO
        else -> TIDAK
    }

    fun mime(nama: String): String = when (eks(nama)) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        "mp4", "m4v" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "3gp" -> "video/3gpp"
        else -> "application/octet-stream"
    }

    /** Nama berkas aman: huruf, angka, titik, garis bawah, strip. */
    fun aman(nama: String): String = nama.filter { it.isLetterOrDigit() || it in "._-" }.take(80)

    fun berkas(nama: String): File? {
        val n = aman(nama)
        if (n.isEmpty() || n != nama) return null
        val f = File(dir, n)
        return if (f.isFile) f else null
    }

    fun daftar(): List<File> =
        dir.listFiles()?.filter { it.isFile && jenis(it.name) != TIDAK }?.sortedByDescending { it.lastModified() }.orEmpty()

    /** Simpan unggahan; mengembalikan nama berkas di TV. */
    fun simpan(sementara: File, namaAsli: String): String {
        val e = eks(namaAsli)
        val dasar = aman(namaAsli.substringBeforeLast('.')).replace(".", "_").take(40).ifBlank { "media" }
        var nama = "$dasar.$e"
        var n = 2
        while (File(dir, nama).exists()) nama = "$dasar-${n++}.$e"
        val tujuan = File(dir, nama)
        if (!sementara.renameTo(tujuan)) {
            sementara.copyTo(tujuan, overwrite = true)
            sementara.delete()
        }
        return nama
    }

    fun hapus(nama: String): Boolean = berkas(nama)?.delete() ?: false

    fun sisaRuang(): Long = dir.usableSpace
}
