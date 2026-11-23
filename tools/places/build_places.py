"""
Build the offline place database for the app.

  cities.tsv     GeoNames cities with population >= 15,000 (CC BY 4.0, geonames.org)
  landmarks.tsv  Famous photo destinations; coordinates from Wikipedia, time zone = nearest city

    python3 tools/places/build_places.py GEONAMES_DIR app/src/main/assets
"""
import json, math, os, sys, time, unicodedata, urllib.parse, urllib.request

UA = "CameraCoach-build-script/1.0 (offline photography learning app)"

LANDMARKS = [
    # (Wikipedia title, display name, short note)
    ("Yosemite Valley", "Yosemite Valley", "USA"), ("Grand Canyon Village, Arizona", "Grand Canyon (South Rim)", "USA"),
    ("Monument Valley", "Monument Valley", "USA"), ("Antelope Canyon", "Antelope Canyon", "USA"),
    ("Old Faithful", "Yellowstone (Old Faithful)", "USA"), ("Arches National Park", "Arches National Park", "USA"),
    ("Zion National Park", "Zion National Park", "USA"), ("Death Valley", "Death Valley", "USA"),
    ("Big Sur", "Big Sur", "USA"), ("Haleakalā National Park", "Haleakalā", "USA"),
    ("Moraine Lake", "Moraine Lake (Banff)", "Canada"), ("Lake Louise (Alberta)", "Lake Louise", "Canada"),
    ("Niagara Falls", "Niagara Falls", "USA / Canada"),
    ("Iguazu Falls", "Iguazú Falls", "Argentina / Brazil"), ("Machu Picchu", "Machu Picchu", "Peru"),
    ("Salar de Uyuni", "Salar de Uyuni", "Bolivia"), ("Fitz Roy", "Fitz Roy (El Chaltén)", "Argentina"),
    ("Torres del Paine National Park", "Torres del Paine", "Chile"), ("San Pedro de Atacama", "San Pedro de Atacama", "Chile"),
    ("Valle de la Luna (Chile)", "Valle de la Luna", "Chile"), ("El Tatio", "El Tatio geysers", "Chile"),
    ("Osorno (volcano)", "Osorno volcano", "Chile"), ("Easter Island", "Easter Island", "Chile"),
    ("Jökulsárlón", "Jökulsárlón glacier lagoon", "Iceland"), ("Kirkjufell", "Kirkjufell", "Iceland"),
    ("Skógafoss", "Skógafoss", "Iceland"), ("Seljalandsfoss", "Seljalandsfoss", "Iceland"),
    ("Reine", "Reine (Lofoten)", "Norway"), ("Geirangerfjord", "Geirangerfjord", "Norway"),
    ("Abisko", "Abisko (northern lights)", "Sweden"), ("Longyearbyen", "Longyearbyen (Svalbard)", "Norway"),
    ("Glen Coe", "Glen Coe", "Scotland"), ("Old Man of Storr", "Old Man of Storr (Skye)", "Scotland"),
    ("Cliffs of Moher", "Cliffs of Moher", "Ireland"), ("Mont-Saint-Michel", "Mont-Saint-Michel", "France"),
    ("Neuschwanstein Castle", "Neuschwanstein Castle", "Germany"), ("Hallstatt", "Hallstatt", "Austria"),
    ("Tre Cime di Lavaredo", "Tre Cime di Lavaredo (Dolomites)", "Italy"), ("Matterhorn", "Matterhorn (Zermatt)", "Switzerland"),
    ("Cinque Terre", "Cinque Terre", "Italy"), ("Lake Bled", "Lake Bled", "Slovenia"),
    ("Plitvice Lakes National Park", "Plitvice Lakes", "Croatia"), ("Oia, Greece", "Oia (Santorini)", "Greece"),
    ("Göreme", "Göreme (Cappadocia)", "Turkey"), ("Teide", "Teide (Tenerife)", "Spain"),
    ("Petra", "Petra", "Jordan"), ("Wadi Rum", "Wadi Rum", "Jordan"),
    ("Giza pyramid complex", "Pyramids of Giza", "Egypt"), ("Merzouga", "Merzouga (Sahara dunes)", "Morocco"),
    ("Serengeti National Park", "Serengeti", "Tanzania"), ("Victoria Falls", "Victoria Falls", "Zambia / Zimbabwe"),
    ("Kruger National Park", "Kruger National Park", "South Africa"), ("Table Mountain", "Table Mountain", "South Africa"),
    ("Sossusvlei", "Sossusvlei / Deadvlei", "Namibia"),
    ("Taj Mahal", "Taj Mahal", "India"), ("Everest base camps", "Everest Base Camp", "Nepal"),
    ("Hạ Long Bay", "Hạ Long Bay", "Vietnam"), ("Angkor Wat", "Angkor Wat", "Cambodia"),
    ("Bagan", "Bagan", "Myanmar"), ("Lake Kawaguchi", "Lake Kawaguchi (Mount Fuji)", "Japan"),
    ("Zhangjiajie National Forest Park", "Zhangjiajie", "China"), ("Yangshuo County", "Yangshuo (Li River)", "China"),
    ("Tegallalang", "Tegallalang rice terraces (Bali)", "Indonesia"), ("Mount Bromo", "Mount Bromo", "Indonesia"),
    ("Uluru", "Uluru", "Australia"), ("Twelve Apostles (Victoria)", "Twelve Apostles", "Australia"),
    ("Whitehaven Beach", "Whitehaven Beach", "Australia"), ("Milford Sound", "Milford Sound", "New Zealand"),
    ("Aoraki / Mount Cook", "Aoraki / Mount Cook", "New Zealand"), ("Lake Tekapo", "Lake Tekapo", "New Zealand"),
]

def norm(s):
    s = unicodedata.normalize("NFKD", s)
    return "".join(c for c in s if not unicodedata.combining(c)).lower()

def wiki_coords(titles):
    out = {}
    for i in range(0, len(titles), 40):
        chunk = titles[i:i + 40]
        q = urllib.parse.urlencode({"action": "query", "format": "json", "prop": "coordinates", "colimit": "max", "redirects": 1, "titles": "|".join(chunk)})
        req = urllib.request.Request("https://en.wikipedia.org/w/api.php?" + q, headers={"User-Agent": UA})
        for attempt in range(8):
            try:
                data = json.load(urllib.request.urlopen(req, timeout=60))
                break
            except Exception:
                time.sleep(5 + attempt * 5)
        else:
            raise RuntimeError("Wikipedia API unavailable")
        redirect = {r["from"]: r["to"] for r in data["query"].get("redirects", [])}
        normalized = {n["from"]: n["to"] for n in data["query"].get("normalized", [])}
        by_title = {p["title"]: p for p in data["query"]["pages"].values()}
        for t in chunk:
            tt = redirect.get(normalized.get(t, t), normalized.get(t, t))
            p = by_title.get(tt)
            if p and p.get("coordinates"):
                c = p["coordinates"][0]
                out[t] = (c["lat"], c["lon"])
        time.sleep(0.5)
    return out

def main():
    geo, out = sys.argv[1], sys.argv[2]
    os.makedirs(out, exist_ok=True)
    countries = {}
    for line in open(os.path.join(geo, "countryInfo.txt"), encoding="utf-8"):
        if line.startswith("#"):
            continue
        f = line.rstrip("\n").split("\t")
        countries[f[0]] = f[4]
    rows = []
    for line in open(os.path.join(geo, "cities15000.txt"), encoding="utf-8"):
        f = line.rstrip("\n").split("\t")
        name, ascii_, lat, lon, cc, pop, tz = f[1], f[2], float(f[4]), float(f[5]), f[8], int(f[14] or 0), f[17]
        if not tz:
            continue
        rows.append((pop, name, ascii_, cc, lat, lon, tz))
    rows.sort(key=lambda r: -r[0])
    with open(os.path.join(out, "cities.tsv"), "w", encoding="utf-8") as fo:
        for pop, name, ascii_, cc, lat, lon, tz in rows:
            fo.write(f"{name}\t{norm(ascii_ or name)}\t{countries.get(cc, cc)}\t{lat:.4f}\t{lon:.4f}\t{tz}\t{pop}\n")
    # landmarks
    coords = wiki_coords([t for t, _, _ in LANDMARKS])
    missing = [t for t, _, _ in LANDMARKS if t not in coords]
    if missing:
        print("missing coordinates:", missing)

    def nearest_tz(lat, lon):
        best, bd = None, 1e9
        for pop, name, ascii_, cc, la, lo, tz in rows:
            d = (la - lat) ** 2 + ((lo - lon) * math.cos(math.radians(lat))) ** 2
            if d < bd:
                best, bd = tz, d
        return best

    with open(os.path.join(out, "landmarks.tsv"), "w", encoding="utf-8") as fo:
        for t, name, country in LANDMARKS:
            if t not in coords:
                continue
            lat, lon = coords[t]
            fo.write(f"{name}\t{norm(name)}\t{country}\t{lat:.4f}\t{lon:.4f}\t{nearest_tz(lat, lon)}\t0\n")
    print(len(rows), "cities;", len(LANDMARKS) - len(missing), "landmarks")

if __name__ == "__main__":
    main()
