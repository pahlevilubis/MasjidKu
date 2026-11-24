package com.masjidku.tv.inti;

/**
 * Menentukan layar yang harus tampil pada detik tertentu:
 * NORMAL -> MENJELANG (sebelum adzan) -> ADZAN -> IQAMAH (hitung mundur) -> SHOLAT -> NORMAL.
 * Hari Jumat (bila aktif) untuk Dzuhur: ADZAN -> KHUTBAH -> SHOLAT.
 *
 * Indeks sholat: 0 Subuh, 1 Dzuhur, 2 Ashar, 3 Maghrib, 4 Isya.
 */
public final class Fase {

    public static final int NORMAL = 0, MENJELANG = 1, ADZAN = 2, IQAMAH = 3, SHOLAT = 4, KHUTBAH = 5;
    public static final String[] NAMA_SHOLAT = {"Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya"};

    /** Pengaturan durasi (menit) per sholat. */
    public static final class Atur {
        public int[] sebelum = {5, 5, 5, 5, 5};
        public int[] adzan = {4, 3, 3, 3, 3};
        public int[] iqamah = {10, 7, 7, 5, 7};
        public int[] sholat = {10, 12, 12, 10, 12};
        public boolean jumat = false;   // hari ini Jumat dan mode Jumat aktif
        public int khutbah = 30;
        public int sholatJumat = 15;
    }

    public int mode = NORMAL;
    public int sholat = -1;          // sholat yang sedang berlangsung (selain NORMAL)
    public int sisaDetik = 0;        // sisa durasi layar saat ini
    public int lamaDetik = 0;        // total durasi layar saat ini
    public int berikut = 0;          // sholat berikutnya (untuk NORMAL & penanda kartu)
    public int detikMenujuBerikut = 0;
    public boolean berikutBesok = false;

    /** Detik yang sudah berlalu sejak layar ini dimulai. */
    public int berlalu() { return lamaDetik - sisaDetik; }

    /**
     * @param detik       detik sejak 00:00 hari ini (waktu setempat)
     * @param menit       waktu 5 sholat hari ini dalam menit (Subuh..Isya)
     * @param subuhBesok  waktu Subuh besok (menit)
     */
    public static Fase hitung(int detik, int[] menit, int subuhBesok, Atur a) {
        Fase f = new Fase();

        // sholat berikutnya
        f.berikut = -1;
        for (int i = 0; i < 5; i++) {
            if (menit[i] * 60 > detik) { f.berikut = i; break; }
        }
        if (f.berikut < 0) {
            f.berikut = 0;
            f.berikutBesok = true;
            f.detikMenujuBerikut = (86400 - detik) + subuhBesok * 60;
        } else {
            f.detikMenujuBerikut = menit[f.berikut] * 60 - detik;
        }

        for (int i = 0; i < 5; i++) {
            int mulai = menit[i] * 60;
            boolean jumat = a.jumat && i == 1;

            int[] mode = new int[4];
            int[] lama = new int[4];
            int n = 0;
            mode[n] = ADZAN;   lama[n++] = Math.max(0, a.adzan[i]) * 60;
            if (jumat) {
                mode[n] = KHUTBAH; lama[n++] = Math.max(0, a.khutbah) * 60;
                mode[n] = SHOLAT;  lama[n++] = Math.max(0, a.sholatJumat) * 60;
            } else {
                mode[n] = IQAMAH;  lama[n++] = Math.max(0, a.iqamah[i]) * 60;
                mode[n] = SHOLAT;  lama[n++] = Math.max(0, a.sholat[i]) * 60;
            }

            // menjelang adzan
            int pra = Math.max(0, a.sebelum[i]) * 60;
            if (pra > 0 && detik >= mulai - pra && detik < mulai) {
                return isi(f, MENJELANG, i, mulai - detik, pra);
            }
            int t = mulai;
            for (int k = 0; k < n; k++) {
                if (lama[k] <= 0) continue;
                if (detik >= t && detik < t + lama[k]) {
                    return isi(f, mode[k], i, t + lama[k] - detik, lama[k]);
                }
                t += lama[k];
            }
        }
        return f;
    }

    private static Fase isi(Fase f, int mode, int sholat, int sisa, int lama) {
        f.mode = mode;
        f.sholat = sholat;
        f.sisaDetik = sisa;
        f.lamaDetik = lama;
        return f;
    }

    /** "HH:MM:SS" atau "MM:SS" bila kurang dari 1 jam. */
    public static String hitungMundur(int detik) {
        if (detik < 0) detik = 0;
        int j = detik / 3600, m = (detik % 3600) / 60, s = detik % 60;
        String mm = (m < 10 ? "0" : "") + m, ss = (s < 10 ? "0" : "") + s;
        if (j > 0) return (j < 10 ? "0" : "") + j + ":" + mm + ":" + ss;
        return mm + ":" + ss;
    }
}
