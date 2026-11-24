package com.masjidku.tv.inti;

/**
 * Perhitungan waktu sholat (hisab) dengan kriteria Kementerian Agama RI.
 *
 * Kriteria bawaan (sudah dicocokkan dengan jadwal Bimas Islam Kemenag):
 *  - Subuh  : matahari -20 derajat, Isya : -18 derajat
 *  - Terbit & Maghrib : matahari -1 derajat
 *  - Dhuha  : matahari +4,5 derajat
 *  - Ashar  : panjang bayangan = 1 x tinggi benda + bayangan saat Dzuhur (Syafi'i)
 *  - Ihtiyath 2 menit (Dzuhur 2 menit + 1 menit lewat piringan matahari),
 *    detik dibulatkan ke atas; Terbit dikurangi ihtiyath lalu dibulatkan ke bawah
 *  - Imsak = Subuh - 10 menit
 *
 * Semua hasil dalam MENIT sejak tengah malam waktu setempat (zona dari pengaturan).
 */
public final class Hisab {

    public static final int IMSAK = 0, SUBUH = 1, TERBIT = 2, DHUHA = 3,
            DZUHUR = 4, ASHAR = 5, MAGHRIB = 6, ISYA = 7;
    public static final int JUMLAH = 8;

    public static final class Param {
        public double lintang = -6.1754;   // derajat, selatan negatif
        public double bujur = 106.8272;    // derajat, timur positif
        public double zona = 7;            // jam (WIB 7, WITA 8, WIT 9)
        public double sudutSubuh = 20;
        public double sudutIsya = 18;
        public int ihtiyath = 2;
        public int mazhabAshar = 1;        // 1 = Syafi'i/jumhur, 2 = Hanafi
        public int imsakMenit = 10;
        public double tinggiDhuha = 4.5;
        public double tinggiTerbenam = -1.0;
        public int[] koreksi = new int[JUMLAH];
    }

    private Hisab() {}

    /** Menghasilkan 8 waktu (menit sejak 00:00) untuk tanggal Masehi y-m-d. */
    public static int[] hitung(int y, int m, int d, Param p) {
        final double lat = p.lintang;
        final double jd = julian(y, m, d) - p.bujur / 360.0;

        // tebakan awal (jam waktu matahari lokal), lalu diperhalus 3 kali
        double subuh = 5, terbit = 6, dhuha = 6.5, dzuhur = 12, ashar = 15, maghrib = 18, isya = 19;
        for (int i = 0; i < 3; i++) {
            subuh = transit(jd, subuh) - sudutJam(jd, lat, -p.sudutSubuh, subuh);
            terbit = transit(jd, terbit) - sudutJam(jd, lat, p.tinggiTerbenam, terbit);
            dhuha = transit(jd, dhuha) - sudutJam(jd, lat, p.tinggiDhuha, dhuha);
            dzuhur = transit(jd, dzuhur);
            ashar = transit(jd, ashar) + sudutJam(jd, lat, tinggiAshar(jd, lat, p.mazhabAshar, ashar), ashar);
            maghrib = transit(jd, maghrib) + sudutJam(jd, lat, p.tinggiTerbenam, maghrib);
            isya = transit(jd, isya) + sudutJam(jd, lat, -p.sudutIsya, isya);
        }

        final double geser = p.zona - p.bujur / 15.0; // ke jam waktu setempat
        final int ih = p.ihtiyath;
        int[] h = new int[JUMLAH];
        h[SUBUH] = atas((subuh + geser) * 60 + ih);
        h[TERBIT] = bawah((terbit + geser) * 60 - ih);
        h[DHUHA] = atas((dhuha + geser) * 60 + ih);
        h[DZUHUR] = atas((dzuhur + geser) * 60 + ih + 1);
        h[ASHAR] = atas((ashar + geser) * 60 + ih);
        h[MAGHRIB] = atas((maghrib + geser) * 60 + ih);
        h[ISYA] = atas((isya + geser) * 60 + ih);

        int[] k = p.koreksi != null && p.koreksi.length == JUMLAH ? p.koreksi : new int[JUMLAH];
        for (int i = 1; i < JUMLAH; i++) h[i] += k[i];
        h[IMSAK] = h[SUBUH] - p.imsakMenit + k[IMSAK];
        return h;
    }

    // ---------- astronomi ----------

    private static int atas(double menit) { return (int) Math.ceil(menit - 1e-9); }
    private static int bawah(double menit) { return (int) Math.floor(menit + 1e-9); }

    /** Julian Day pukul 00:00 UT. */
    static double julian(int y, int m, int d) {
        if (m <= 2) { y -= 1; m += 12; }
        int a = y / 100;
        int b = 2 - a + a / 4;
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
    }

    /** [deklinasi (derajat), perata waktu (jam)] */
    private static double[] matahari(double jd) {
        double hari = jd - 2451545.0;
        double g = normal(357.529 + 0.98560028 * hari, 360);
        double q = normal(280.459 + 0.98564736 * hari, 360);
        double l = normal(q + 1.915 * sin(g) + 0.020 * sin(2 * g), 360);
        double e = 23.439 - 0.00000036 * hari;
        double ra = Math.toDegrees(Math.atan2(cos(e) * sin(l), cos(l))) / 15.0;
        double eqt = q / 15.0 - normal(ra, 24);
        double dekl = Math.toDegrees(Math.asin(sin(e) * sin(l)));
        return new double[]{dekl, eqt};
    }

    private static double transit(double jd, double jam) {
        double eqt = matahari(jd + jam / 24.0)[1];
        return normal(12 - eqt, 24);
    }

    /** Sudut jam (dalam jam) saat matahari mencapai ketinggian 'tinggi' derajat. */
    private static double sudutJam(double jd, double lat, double tinggi, double jam) {
        double dekl = matahari(jd + jam / 24.0)[0];
        double x = (sin(tinggi) - sin(lat) * sin(dekl)) / (cos(lat) * cos(dekl));
        if (x > 1) x = 1;
        if (x < -1) x = -1;
        return Math.toDegrees(Math.acos(x)) / 15.0;
    }

    private static double tinggiAshar(double jd, double lat, int faktor, double jam) {
        double dekl = matahari(jd + jam / 24.0)[0];
        double bayang = faktor + Math.tan(Math.toRadians(Math.abs(lat - dekl)));
        return Math.toDegrees(Math.atan(1.0 / bayang));
    }

    private static double sin(double d) { return Math.sin(Math.toRadians(d)); }
    private static double cos(double d) { return Math.cos(Math.toRadians(d)); }
    private static double normal(double a, double b) { return a - b * Math.floor(a / b); }

    /** Format menit sejak 00:00 menjadi "HH:MM". */
    public static String jam(int menit) {
        int m = ((menit % 1440) + 1440) % 1440;
        int j = m / 60, mm = m % 60;
        return (j < 10 ? "0" : "") + j + ":" + (mm < 10 ? "0" : "") + mm;
    }
}
