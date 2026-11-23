"""
Find community-rated photos (Wikimedia Commons Featured / Quality pictures) with
EXIF camera settings and a free license, for each photo situation in the app.

    python3 tools/photos/search.py OUT_DIR        # writes candidates.json + contact sheets
"""
import json, os, sys, time, urllib.parse, urllib.request, io, html, re
from PIL import Image, ImageDraw

UA = "CameraCoach-build-script/1.0 (offline photography learning app)"
API = "https://commons.wikimedia.org/w/api.php"
OK_LICENSES = ("CC BY-SA 4.0", "CC BY-SA 3.0", "CC BY-SA 2.5", "CC BY-SA 2.0", "CC BY 4.0", "CC BY 3.0", "CC BY 2.5", "CC BY 2.0", "CC0", "Public domain")

QUERIES = {
    "landscape": ["mountain lake reflection", "mountain valley landscape"],
    "golden": ["sunset landscape", "sunrise mountains"],
    "bright": ["sand dunes desert", "snow winter landscape sun", "tropical beach"],
    "water": ["waterfall long exposure", "waterfall"],
    "forest": ["foggy forest", "misty forest morning"],
    "astro": ["Milky Way night sky", "starry sky night landscape"],
    "aurora": ["aurora borealis", "northern lights"],
    "city": ["blue hour skyline", "city at night skyline"],
    "trails": ["light trails long exposure", "star trails"],
    "fireworks": ["fireworks"],
    "indoor": ["church interior", "cathedral interior nave"],
    "wildlife": ["fox wildlife", "deer forest", "elephant"],
    "birds": ["bird in flight", "eagle flying", "gull flying"],
    "action": ["surfing wave", "cycling race", "ski jumping"],
    "pets": ["dog portrait", "cat portrait"],
    "portrait": ["portrait of a woman", "portrait of a man"],
    "street": ["street market", "old town street"],
    "architecture": ["architecture facade", "skyscraper"],
    "food": ["food dish", "cake dessert"],
    "macro": ["macro insect", "flower close-up", "dragonfly"],
}

def api(params):
    params = dict(params, format="json")
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                return json.load(r)
        except Exception as e:
            time.sleep(2 + attempt * 3)
    raise RuntimeError("API failed: " + url)

def meta(ii, key):
    for m in ii.get("commonmetadata", []) or []:
        if m.get("name") == key:
            return m.get("value")
    return None

def ext(ii, key):
    v = (ii.get("extmetadata") or {}).get(key, {}).get("value")
    return v

def strip_html(s):
    if s is None:
        return None
    s = re.sub(r"<[^>]+>", "", str(s))
    return html.unescape(s).strip()

def num(v):
    if v is None:
        return None
    if isinstance(v, (int, float)):
        return float(v)
    s = str(v).strip()
    if "/" in s:
        a, b = s.split("/", 1)
        try:
            return float(a) / float(b)
        except Exception:
            return None
    try:
        return float(s)
    except Exception:
        return None

def candidates(scene, query, cat):
    res = api({
        "action": "query", "generator": "search", "gsrnamespace": 6, "gsrlimit": 40,
        "gsrsearch": f'incategory:"{cat}" {query} filetype:bitmap',
        "prop": "imageinfo", "iiprop": "url|size|extmetadata|commonmetadata|mime", "iiurlwidth": 1280,
    })
    out = []
    for p in (res.get("query", {}).get("pages", {}) or {}).values():
        ii = (p.get("imageinfo") or [{}])[0]
        if ii.get("mime") != "image/jpeg":
            continue
        lic = strip_html(ext(ii, "LicenseShortName")) or ""
        if not any(lic.startswith(x) for x in OK_LICENSES):
            continue
        f = num(meta(ii, "FNumber"))
        t = num(meta(ii, "ExposureTime"))
        iso = meta(ii, "ISOSpeedRatings")
        if isinstance(iso, list):
            iso = iso[0] if iso else None
        if isinstance(iso, dict):
            iso = list(iso.values())[0]
        iso = num(iso)
        fl = num(meta(ii, "FocalLength"))
        model = strip_html(meta(ii, "Model"))
        make = strip_html(meta(ii, "Make"))
        if not (f and t and iso and fl and model):
            continue
        out.append({
            "scene": scene, "query": query, "category": cat,
            "title": p["title"], "page": ii.get("descriptionurl"),
            "thumb": ii.get("thumburl"), "w": ii.get("width"), "h": ii.get("height"),
            "license": lic, "license_url": strip_html(ext(ii, "LicenseUrl")),
            "artist": strip_html(ext(ii, "Artist")),
            "description": strip_html(ext(ii, "ImageDescription")),
            "restrictions": strip_html(ext(ii, "Restrictions")),
            "f": f, "t": t, "iso": iso, "focal": fl,
            "focal35": num(meta(ii, "FocalLengthIn35mmFilm")),
            "make": make, "model": model,
            "lens": strip_html(meta(ii, "LensModel") or meta(ii, "Lens")),
            "date": strip_html(meta(ii, "DateTimeOriginal")),
        })
    return out

def main():
    out_dir = sys.argv[1]
    os.makedirs(out_dir, exist_ok=True)
    allc = {}
    for scene, queries in QUERIES.items():
        seen, picks = set(), []
        for cat in ("Featured pictures on Wikimedia Commons", "Quality images"):
            for q in queries:
                try:
                    for c in candidates(scene, q, cat):
                        if c["title"] not in seen:
                            seen.add(c["title"])
                            c["rated"] = "Featured picture" if cat.startswith("Featured") else "Quality image"
                            picks.append(c)
                except Exception as e:
                    print("  ! ", scene, q, e, file=sys.stderr)
                time.sleep(0.3)
            if len(picks) >= 14:
                break
        picks = picks[:16]
        allc[scene] = picks
        print(f"{scene}: {len(picks)} candidates", flush=True)
    json.dump(allc, open(os.path.join(out_dir, "candidates.json"), "w"), indent=1)

if __name__ == "__main__":
    main()
