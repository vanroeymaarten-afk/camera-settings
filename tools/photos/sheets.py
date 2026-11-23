"""Download small previews of candidates and build one contact sheet per scene (for manual review)."""
import json, os, sys, io, time, urllib.request
from PIL import Image, ImageDraw, ImageFont

UA = "CameraCoach-build-script/1.0 (offline photography learning app)"

def fetch(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    for a in range(4):
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                return r.read()
        except Exception:
            time.sleep(2 + 3 * a)
    return None

def small(url, w=330):
    return url.replace("/1280px-", f"/{w}px-") if "/1280px-" in url else url

def main():
    d = sys.argv[1]
    only = sys.argv[2].split(",") if len(sys.argv) > 2 else None
    cands = json.load(open(os.path.join(d, "candidates.json")))
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 13)
    for scene, items in cands.items():
        if only and scene not in only:
            continue
        cols, tw, th = 4, 330, 250
        rows = (len(items) + cols - 1) // cols
        sheet = Image.new("RGB", (cols * tw, rows * th), "white")
        dr = ImageDraw.Draw(sheet)
        for i, c in enumerate(items):
            raw = fetch(small(c["thumb"]))
            x, y = (i % cols) * tw, (i // cols) * th
            if raw:
                im = Image.open(io.BytesIO(raw)).convert("RGB")
                im.thumbnail((tw - 6, th - 40))
                sheet.paste(im, (x + 3, y + 3))
            t = c["t"]
            ts = f"1/{round(1/t)}" if t < 0.5 else f"{t:g}s"
            dr.text((x + 4, y + th - 36), f"#{i} f/{c['f']:g} {ts} ISO{int(c['iso'])} {c['focal']:g}mm", fill="black", font=font)
            dr.text((x + 4, y + th - 19), (c["model"] or "")[:28] + " | " + c["license"][:12], fill="#555", font=font)
            time.sleep(0.1)
        sheet.save(os.path.join(d, f"sheet_{scene}.jpg"), quality=80)
        print(scene, "ok", flush=True)

if __name__ == "__main__":
    main()
