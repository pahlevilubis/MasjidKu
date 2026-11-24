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

## Build
APK dibangun otomatis oleh GitHub Actions setiap push ke `main`, lalu terbit di halaman **Releases**.
Opsional: isi secret `KEYSTORE_BASE64` dan `KEYSTORE_PASSWORD` agar APK selalu bertanda tangan sama
(pembaruan bisa langsung dipasang tanpa uninstall).

## Struktur
- `app/src/main/java/com/masjidku/tv/inti/` — hisab, kalender, logika fase (Java murni, diuji di CI)
- `app/src/main/java/com/masjidku/tv/` — mesin, jam NTP, suara, panel web, aktivitas
- `app/src/main/java/com/masjidku/tv/ui/` — tampilan TV (Jetpack Compose)
- `app/src/main/assets/panel/index.html` — panel pengaturan untuk HP
- `tools/` — uji otomatis & pembuat ikon
