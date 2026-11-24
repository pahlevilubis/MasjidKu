// Uji otomatis inti MasjidKu: hisab dibandingkan jadwal Kemenag (Jakarta, Feb & Mei 2024) + logika fase.
import com.masjidku.tv.inti.*;
import java.util.*;
import java.nio.file.*;
public class UjiInti {
  static int gagal = 0;
  static void cek(boolean b, String pesan){ if(!b){ gagal++; System.out.println("GAGAL: "+pesan);} }
  public static void main(String[] a) throws Exception {
    // 1. Hisab vs Kemenag Jakarta (Feb & Mei 2024)
    List<String> baris = Files.readAllLines(Paths.get(a[0]));
    Hisab.Param p = new Hisab.Param(); p.lintang=-6.16; p.bujur=106.83; p.zona=7;
    int[] idx = {Hisab.IMSAK,Hisab.SUBUH,Hisab.DHUHA,Hisab.DZUHUR,Hisab.ASHAR,Hisab.MAGHRIB,Hisab.ISYA};
    String[] nm={"imsak","subuh","dhuha","dzuhur","ashar","maghrib","isya"};
    int[][] hit = new int[7][3];
    for (String s: baris){ if (s.startsWith("#") || s.isBlank()) continue; String[] f=s.trim().split("\\s+"); int y=Integer.parseInt(f[0]),m=Integer.parseInt(f[1]),d=Integer.parseInt(f[2]);
      int[] h = Hisab.hitung(y,m,d,p);
      for(int i=0;i<7;i++){ String[] hm=f[3+i].split(":"); int ref=Integer.parseInt(hm[0])*60+Integer.parseInt(hm[1]); int df=h[idx[i]]-ref; hit[i][df+1 >=0 && df+1<3 ? df+1 : 1] += (Math.abs(df)<=1?1:0); if(Math.abs(df)>1){gagal++; System.out.println("selisih>1 "+s+" "+nm[i]+" "+df);} }
    }
    for(int i=0;i<7;i++) System.out.println(nm[i]+": -1="+hit[i][0]+" pas="+hit[i][1]+" +1="+hit[i][2]);
    // 2. Tanggal
    cek(Arrays.equals(Tanggal.masehi(0), new int[]{1970,1,1}), "masehi 0");
    
    // cari hari untuk 2026-11-24
    long h = 0; while(!Arrays.equals(Tanggal.masehi(h), new int[]{2026,11,24})) h++;
    System.out.println("2026-11-24 = hari "+h+" -> "+Tanggal.teksMasehi(h)+" / "+Tanggal.teksHijriah(h));
    cek(Tanggal.hariKe(h)==2, "24 Nov 2026 harus Selasa");
    long r = 0; while(!Arrays.equals(Tanggal.masehi(r), new int[]{2026,2,18})) r++;
    System.out.println("2026-02-18 -> "+Tanggal.teksHijriah(r)+"; 2026-02-19 -> "+Tanggal.teksHijriah(r+1));
    long r2=0; while(!Arrays.equals(Tanggal.masehi(r2), new int[]{2024,2,29})) r2++;
    cek(Tanggal.masehi(r2+1)[1]==3 && Tanggal.masehi(r2+1)[2]==1, "kabisat");
    // 3. Fase
    int[] w = {4*60+30, 12*60, 15*60+15, 18*60, 19*60+10};
    Fase.Atur at = new Fase.Atur();
    Fase f;
    f = Fase.hitung(3*3600, w, 4*60+31, at); cek(f.mode==Fase.NORMAL && f.berikut==0 && f.detikMenujuBerikut==90*60, "normal sebelum subuh "+f.detikMenujuBerikut);
    f = Fase.hitung(12*3600-60, w, 0, at); cek(f.mode==Fase.MENJELANG && f.sholat==1 && f.sisaDetik==60, "menjelang dzuhur");
    f = Fase.hitung(12*3600, w, 0, at); cek(f.mode==Fase.ADZAN && f.sholat==1 && f.sisaDetik==180 && f.berlalu()==0, "adzan dzuhur");
    f = Fase.hitung(12*3600+180, w, 0, at); cek(f.mode==Fase.IQAMAH && f.sisaDetik==7*60, "iqamah dzuhur "+f.mode+" "+f.sisaDetik);
    f = Fase.hitung(12*3600+180+7*60, w, 0, at); cek(f.mode==Fase.SHOLAT && f.sisaDetik==12*60, "sholat dzuhur");
    f = Fase.hitung(12*3600+180+7*60+12*60, w, 0, at); cek(f.mode==Fase.NORMAL && f.berikut==2, "normal setelah dzuhur");
    at.jumat=true;
    f = Fase.hitung(12*3600+200, w, 0, at); cek(f.mode==Fase.KHUTBAH, "khutbah jumat");
    f = Fase.hitung(12*3600+180+30*60+5, w, 0, at); cek(f.mode==Fase.SHOLAT && f.sholat==1, "sholat jumat");
    at.jumat=false;
    f = Fase.hitung(23*3600, w, 4*60+31, at); cek(f.mode==Fase.NORMAL && f.berikut==0 && f.berikutBesok && f.detikMenujuBerikut==3600+4*3600+31*60, "setelah isya ke subuh besok");
    at.sebelum[1]=0; f = Fase.hitung(12*3600-60, w, 0, at); cek(f.mode==Fase.NORMAL, "tanpa menjelang");
    cek(Fase.hitungMundur(3725).equals("01:02:05") && Fase.hitungMundur(65).equals("01:05"), "format hitung mundur");
    cek(Hisab.jam(4*60+5).equals("04:05") && Hisab.jam(-5).equals("23:55"), "format jam");
    System.out.println(gagal==0 ? "SEMUA UJI LULUS" : ("GAGAL: "+gagal));
    System.exit(gagal==0?0:1);
  }
}
