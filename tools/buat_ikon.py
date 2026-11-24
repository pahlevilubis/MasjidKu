"""Membuat ikon aplikasi & banner Android TV MasjidKu (dijalankan sekali, hasil di-commit)."""
import os, sys
from PIL import Image, ImageDraw, ImageFont

RES = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/res"
HIJAU1, HIJAU2 = (10, 53, 38), (4, 27, 19)
EMAS = (233, 196, 106)
FONT = "/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf"

def gradasi(w, h):
    im = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(im)
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(int(HIJAU1[i] * (1 - t) + HIJAU2[i] * t) for i in range(3))
        d.line([(0, y), (w, y)], fill=c)
    return im

def masjid(d, cx, base, s, warna=EMAS):
    """Siluet masjid sederhana: kubah, badan, 2 menara, bulan sabit."""
    # badan
    d.rectangle([cx - 0.62*s, base - 0.55*s, cx + 0.62*s, base], fill=warna)
    # kubah
    d.ellipse([cx - 0.48*s, base - 1.15*s, cx + 0.48*s, base - 0.25*s], fill=warna)
    # puncak kubah
    d.rectangle([cx - 0.03*s, base - 1.35*s, cx + 0.03*s, base - 1.1*s], fill=warna)
    # bulan sabit di puncak (lingkaran dikurangi lingkaran bergeser, lewat mask)
    r = 0.12*s
    W, H = d.im.size
    m = Image.new("L", (W, H), 0)
    md = ImageDraw.Draw(m)
    md.ellipse([cx - r, base - 1.62*s, cx + r, base - 1.62*s + 2*r], fill=255)
    md.ellipse([cx - r + 0.08*s, base - 1.67*s, cx + r + 0.08*s, base - 1.67*s + 2*r], fill=0)
    d._image.paste(warna, (0, 0, W, H), m)
    # menara
    for sx in (-1, 1):
        x = cx + sx * 0.82*s
        d.rectangle([x - 0.08*s, base - 1.25*s, x + 0.08*s, base], fill=warna)
        d.polygon([(x - 0.12*s, base - 1.25*s), (x + 0.12*s, base - 1.25*s), (x, base - 1.5*s)], fill=warna)
    # pintu
    d.rounded_rectangle([cx - 0.13*s, base - 0.33*s, cx + 0.13*s, base], radius=0.12*s, fill=HIJAU2)

def ikon(px):
    k = 4  # supersampling
    W = px * k
    im = gradasi(W, W)
    mask = Image.new("L", (W, W), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, W - 1, W - 1], radius=int(W * 0.22), fill=255)
    d = ImageDraw.Draw(im)
    masjid(d, W / 2, W * 0.80, W * 0.34)
    out = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    return out.resize((px, px), Image.LANCZOS)

def banner():
    k = 4
    w, h = 320 * k, 180 * k
    im = gradasi(w, h)
    d = ImageDraw.Draw(im)
    masjid(d, 70 * k, 140 * k, 58 * k)
    f1 = ImageFont.truetype(FONT, 38 * k)
    f2 = ImageFont.truetype(FONT, 14 * k)
    d.text((128 * k, 56 * k), "MasjidKu", font=f1, fill=EMAS)
    d.text((130 * k, 108 * k), "Jadwal Sholat Digital", font=f2, fill=(247, 243, 232))
    return im.resize((320, 180), Image.LANCZOS)

ukuran = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for nama, px in ukuran.items():
    d = os.path.join(RES, f"mipmap-{nama}")
    os.makedirs(d, exist_ok=True)
    ikon(px).save(os.path.join(d, "ic_launcher.png"))
os.makedirs(os.path.join(RES, "drawable-xhdpi"), exist_ok=True)
banner().save(os.path.join(RES, "drawable-xhdpi", "banner.png"))
ikon(512).save("ikon-512.png")
print("ikon & banner dibuat")
