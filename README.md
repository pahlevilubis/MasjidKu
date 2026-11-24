# MasjidKu — Jadwal Sholat Digital untuk Android TV

Aplikasi layar jadwal sholat masjid yang berjalan langsung di **Android TV / STB Android**.
Diatur sepenuhnya dari **HP lewat browser** (tanpa aplikasi tambahan).

## Fitur
- Jam besar, tanggal Masehi & Hijriah, nama & alamat masjid
- Jadwal Imsak, Subuh, Syuruq, (Dhuha), Dzuhur, Ashar, Maghrib, Isya — **dihitung di TV** dengan kriteria Kemenag RI
  (Subuh −20°, Isya −18°, ihtiyath 2 menit). Diuji terhadap jadwal resmi Bimas Islam Kemenag (Jakarta, 60 hari).
- Hitung mundur ke sholat berikutnya
- Alur otomatis: **menjelang adzan → adzan → hitung mundur iqamah → layar sholat** (redup) → normal
- **Mode Jumat**: adzan → khutbah → sholat Jumat
- Suara: **rekaman adzan** (umum & Subuh), **tarhim** sebelum adzan, atau beep; beep saat iqamah habis & saat Imsak
- Slide informasi bergantian, **petugas Jumat**, **laporan kas**, **teks berjalan**
- 4 tema warna, redup otomatis malam hari
- Jam diambil dari **internet (NTP)**, tidak bergantung jam TV
- Muncul sendiri saat TV dinyalakan, saat adzan, dan kembali ke jadwal bila TV dipakai aplikasi lain
- Panel HP: `http://<ip-tv>:8080` + PIN (PIN & kode QR tampil di TV saat tombol **OK** remote ditekan)
- **Pembaruan lewat internet**: TV memeriksa `versi.json` di rilis terbaru tiap 6 jam, mengunduh APK
  (cek ukuran, SHA-256, sertifikat), lalu memasang saat layar senggang (tanpa konfirmasi di Android 12+
  setelah pembaruan pertama; selain itu pengurus menekan Pasang di TV)

## Build
APK dibangun otomatis oleh GitHub Actions setiap push ke `main`, lalu terbit di halaman **Releases**.
**Wajib untuk pembaruan otomatis:** isi secret `KEYSTORE_BASE64` dan `KEYSTORE_PASSWORD` agar semua APK
bertanda tangan sama. Tanpa itu APK memakai kunci debug acak dan TV hanya bisa diperbarui manual.
Kode versi = nomor run GitHub Actions; `versi.json` ikut terbit di setiap rilis.

## Struktur
- `app/src/main/java/com/masjidku/tv/inti/` — hisab, kalender, logika fase (Java murni, diuji di CI)
- `app/src/main/java/com/masjidku/tv/` — mesin, jam NTP, suara, panel web, aktivitas
- `app/src/main/java/com/masjidku/tv/ui/` — tampilan TV (Jetpack Compose)
- `app/src/main/assets/panel/index.html` — panel pengaturan untuk HP
- `tools/` — uji otomatis & pembuat ikon
