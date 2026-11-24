package com.masjidku.tv.inti;

/**
 * Kalender Masehi & Hijriah tanpa bergantung pada zona waktu sistem TV.
 * "hari" = jumlah hari sejak 1 Januari 1970 menurut jam setempat (zona dari pengaturan).
 */
public final class Tanggal {

    public static final String[] HARI = {"Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"};
    public static final String[] BULAN = {"Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
    public static final String[] BULAN_HIJRIAH = {"Muharram", "Safar", "Rabiul Awal", "Rabiul Akhir",
            "Jumadil Awal", "Jumadil Akhir", "Rajab", "Sya'ban", "Ramadhan", "Syawal",
            "Dzulqa'dah", "Dzulhijjah"};

    private Tanggal() {}

    /** {tahun, bulan(1-12), tanggal} dari nomor hari sejak 1970-01-01. */
    public static int[] masehi(long hari) {
        long z = hari + 719468;
        long era = (z >= 0 ? z : z - 146096) / 146097;
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp < 10 ? mp + 3 : mp - 9;
        return new int[]{(int) (y + (m <= 2 ? 1 : 0)), (int) m, (int) d};
    }

    /** 0 = Minggu ... 5 = Jumat, 6 = Sabtu */
    public static int hariKe(long hari) {
        long r = (hari + 4) % 7;
        return (int) (r < 0 ? r + 7 : r);
    }

    /** {tahun, bulan(1-12), tanggal} Hijriah (kalender tabular/urfi). */
    public static int[] hijriah(long hari) {
        long jdn = hari + 2440588L;
        long l = jdn - 1948440 + 10632;
        long n = (l - 1) / 10631;
        l = l - 10631 * n + 354;
        long j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238);
        l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29;
        long m = (24 * l) / 709;
        long d = l - (709 * m) / 24;
        long y = 30 * n + j - 30;
        return new int[]{(int) y, (int) m, (int) d};
    }

    public static String teksMasehi(long hari) {
        int[] t = masehi(hari);
        return HARI[hariKe(hari)] + ", " + t[2] + " " + BULAN[t[1] - 1] + " " + t[0];
    }

    public static String teksHijriah(long hari) {
        int[] h = hijriah(hari);
        return h[2] + " " + BULAN_HIJRIAH[h[1] - 1] + " " + h[0] + " H";
    }
}
