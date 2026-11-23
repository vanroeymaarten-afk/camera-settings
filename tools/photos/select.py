"""
Download the chosen photos, convert to WebP for the app and write the Kotlin manifest.

    python3 tools/photos/select.py CANDIDATES_JSON
"""
import io, json, os, re, sys, time, urllib.request
from PIL import Image

UA = "CameraCoach-build-script/1.0 (offline photography learning app)"
RES = "app/src/main/res/drawable-nodpi"
KT = "app/src/main/java/com/camerasettings/app/data/Photos.kt"

# scene -> list of (candidate index, short title)
CHOICES = {
    "landscape": [(9, "Mountain lake in the Sayan Mountains, Russia"), (15, "Krishansar Lake, Kashmir, India")],
    "golden": [(5, "Sunset over rice paddies, Laos"), (12, "Cliffs of Olkhon Island at sunset, Lake Baikal")],
    "bright": [(1, "Dead camel thorn tree in Deadvlei, Namibia"), (13, "Ice-covered shrub in the winter sun, Germany")],
    "water": [(13, "Kuang Si Falls, Laos"), (9, "Goðafoss waterfall, Iceland")],
    "forest": [(0, "Foggy Fanal forest, Madeira"), (1, "Foggy reflections on Loch Tummel, Scotland")],
    "astro": [(2, "Milky Way, a meteor and the Andromeda galaxy"), (1, "Milky Way over Oeschinensee, Switzerland")],
    "aurora": [(0, "Northern lights over Mývatn, Iceland"), (9, "Aurora in Abisko, Sweden")],
    "city": [(13, "Midtown Manhattan at blue hour"), (2, "Canary Wharf at night, London")],
    "trails": [(2, "Light trails on the Bir-Hakeim bridge, Paris"), (15, "Light trails in Gramado, Brazil")],
    "fireworks": [(8, "Fireworks over the Ponte Vecchio, Florence"), (0, "National Day fireworks, Singapore")],
    "indoor": [(0, "St. Salvator's Cathedral, Bruges"), (11, "St Andrew's Church, Roanoke, USA")],
    "wildlife": [(1, "Red fox drinking from a lake, France"), (12, "Elephant mother with her calf")],
    "birds": [(0, "Greater flamingo in flight, Camargue"), (9, "Brown pelican in flight, California")],
    "action": [(6, "Surfer at Lobitos, Peru"), (2, "Surfer inside a wave, California")],
    "pets": [(4, "Calico cat in Assisi, Italy"), (1, "Golden retriever on the run")],
    "portrait": [(6, "Man reading the morning paper, Kathmandu")],
    "street": [(5, "Crosswalk on Market Street, San Francisco"), (14, "Souq in Luxor, Egypt")],
    "architecture": [(11, "Residence Bassac at blue hour, Laos"), (7, "Church of Göttweig Abbey, Austria")],
    "food": [(1, "A dish of blueberries"), (10, "Spicy egg curry")],
    "macro": [(9, "Black darter dragonfly"), (15, "Damselfly on a stem")],
}

def fetch(url):
    """Download with curl (Wikimedia throttles some HTTP/1.1 clients much harder)."""
    import subprocess
    for a in range(8):
        r = subprocess.run(["curl", "-sSf", "--max-time", "120", "-A", UA, url], capture_output=True)
        if r.returncode == 0 and len(r.stdout) > 10000:
            return r.stdout
        time.sleep(min(60, 5 + 8 * a))
    raise RuntimeError("download failed: " + url)

def camera_name(c):
    make = (c.get("make") or "").strip()
    model = (c.get("model") or "").strip()
    short = make.split()[0].title() if make else ""
    short = {"Nikon": "Nikon", "Canon": "Canon", "Sony": "Sony", "Fujifilm": "Fujifilm", "Olympus": "Olympus", "Panasonic": "Panasonic", "Om": "OM System"}.get(short, short)
    if short and model.lower().startswith(short.lower()):
        return model
    return (short + " " + model).strip()

def kstr(s):
    return '"' + (s or "").replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$").replace("\n", " ").strip() + '"'

def main():
    cands = json.load(open(sys.argv[1]))
    os.makedirs(RES, exist_ok=True)
    entries = []
    for scene, picks in CHOICES.items():
        for n, (idx, title) in enumerate(picks):
            c = cands[scene][idx]
            pid = f"{scene}_{n + 1}"
            out = os.path.join(RES, f"photo_{pid}.webp")
            if not os.path.exists(out):
                url = c["thumb"].split("?")[0].replace("https://thumb.wikimedia.org/", "https://upload.wikimedia.org/")
                raw = fetch(url)
                im = Image.open(io.BytesIO(raw)).convert("RGB")
                im.thumbnail((1080, 1080), Image.LANCZOS)
                im.save(out, "WEBP", quality=74, method=6)
                time.sleep(1)
            im = Image.open(out)
            artist = re.sub(r"\s+", " ", (c["artist"] or "Unknown")).strip()
            artist = artist.split(" (")[0][:60]
            def d(x):
                return repr(float(x))
            entries.append(
                f'        Photo({kstr(pid)}, {kstr(scene)}, {kstr(title)}, R.drawable.photo_{pid}, {im.width}, {im.height},\n'
                f'            {d(c["f"])}, {d(c["t"])}, {int(c["iso"])}, {d(c["focal"])}, {int(c["focal35"]) if c.get("focal35") else 0},\n'
                f'            {kstr(camera_name(c))}, {kstr(c.get("lens") or "")},\n'
                f'            {kstr(artist)}, {kstr(c["license"])}, {kstr(c.get("license_url") or "")}, {kstr(c["page"])}, {kstr(c["rated"])}),'
            )
            print(pid, im.size, os.path.getsize(out) // 1024, "KB", flush=True)
    kt = '''package com.camerasettings.app.data

import com.camerasettings.app.R

/**
 * Community-rated example photos (Wikimedia Commons Featured / Quality pictures) with the
 * camera settings read from their EXIF data. Generated by tools/photos/select.py.
 * Every photo is used under its free license; see [author], [license] and [source].
 */
data class Photo(
    val id: String,
    val sceneId: String,
    val title: String,
    val res: Int,
    val width: Int,
    val height: Int,
    val aperture: Double,
    val shutter: Double,
    val iso: Int,
    val focal: Double,
    /** 35 mm-equivalent focal length if the camera recorded it, else 0. */
    val focal35: Int,
    val camera: String,
    val lens: String,
    val author: String,
    val license: String,
    val licenseUrl: String,
    val source: String,
    val rating: String,
)

object Photos {
    val all: List<Photo> = listOf(
''' + "\n".join(entries) + '''
    )

    fun forScene(sceneId: String) = all.filter { it.sceneId == sceneId }
    fun byId(id: String?) = all.firstOrNull { it.id == id }
}
'''
    open(KT, "w").write(kt)
    print(len(entries), "photos")

if __name__ == "__main__":
    main()
