"""
Render all example illustrations for the app.

    python3 tools/art/render.py               # writes app/src/main/res/drawable-nodpi/*.webp
    python3 tools/art/render.py --preview DIR # also writes before|after preview PNGs to DIR
    python3 tools/art/render.py --only osorno --preview DIR
"""
import argparse
import os
import sys
import time

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import scenes  # noqa: E402
from paintkit import as_raw, edit, save  # noqa: E402

# id -> (painter, edit settings). Edit settings mirror the Lightroom presets in the app.
LOOKS = {
    "golden": dict(warmth=0.55, contrast=0.28, vibrance=0.3, shadows_tint="#3a2f5a", highlights_tint="#ffb060", split=0.28, vig=0.32),
    "night": dict(warmth=-0.15, contrast=0.32, vibrance=0.35, shadows_tint="#14213d", highlights_tint="#ffd9b0", split=0.18, fade=0.0, vig=0.4, grain_amt=0.008),
    "lakes": dict(warmth=0.3, contrast=0.22, vibrance=0.25, shadows_tint="#1f4a4a", highlights_tint="#ffc890", split=0.22, vig=0.28, clarity=0.25),
    "city": dict(warmth=0.15, contrast=0.3, vibrance=0.3, shadows_tint="#16264f", highlights_tint="#ffb35c", split=0.3, vig=0.3),
    "color": dict(warmth=0.3, contrast=0.25, vibrance=0.45, shadows_tint="#20304a", highlights_tint="#ffcf80", split=0.15, vig=0.22, clarity=0.2),
    "moody": dict(warmth=0.35, contrast=0.18, vibrance=0.1, shadows_tint="#20352c", highlights_tint="#ffd59a", split=0.3, fade=0.06, vig=0.4, grain_amt=0.018),
    "altiplano": dict(warmth=0.25, contrast=0.3, vibrance=0.35, shadows_tint="#1b2f5c", highlights_tint="#ffcf86", split=0.18, vig=0.25, clarity=0.3),
}

SHOTS = {
    "osorno": (scenes.osorno, "lakes"),
    "valle_luna": (scenes.valle_luna, "golden"),
    "milky_way": (scenes.milky_way, "night"),
    "el_tatio": (scenes.el_tatio, "golden"),
    "flamingos": (scenes.flamingos, "golden"),
    "altiplano": (scenes.altiplano, "altiplano"),
    "santiago": (scenes.santiago, "city"),
    "valparaiso": (scenes.valparaiso, "color"),
    "condor": (scenes.condor, "altiplano"),
    "petrohue": (scenes.petrohue, "lakes"),
    "palafitos": (scenes.palafitos, "color"),
    "forest": (scenes.forest, "moody"),
    "penguins": (scenes.penguins, "moody"),
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview")
    ap.add_argument("--only")
    ap.add_argument("--out", default=os.path.join(os.path.dirname(__file__), "../../app/src/main/res/drawable-nodpi"))
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    if a.preview:
        os.makedirs(a.preview, exist_ok=True)
    for sid, (painter, look) in SHOTS.items():
        if a.only and sid not in a.only.split(","):
            continue
        t = time.time()
        base = np.clip(painter(), 0, 1)
        before = as_raw(base)
        after = edit(base, **LOOKS[look])
        save(before, os.path.join(a.out, f"ex_{sid}_before.webp"), quality=82)
        save(after, os.path.join(a.out, f"ex_{sid}_after.webp"), quality=86)
        save(after, os.path.join(a.out, f"thumb_{sid}.webp"), size=(480, 320), quality=84)
        if a.preview:
            pair = np.concatenate([before, np.ones((before.shape[0], 12, 3), np.float32), after], axis=1)
            Image.fromarray((np.clip(pair, 0, 1) * 255).astype(np.uint8)).resize((1206, 400)).save(os.path.join(a.preview, f"{sid}.png"))
        print(f"{sid}: {time.time() - t:.1f}s")


if __name__ == "__main__":
    main()
