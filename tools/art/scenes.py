"""
Painted example scenes for the Shot guide. Each function returns a 'natural'
base image; render.py derives the flat RAW-look ('before') and warm edit ('after').
"""
import numpy as np
from paintkit import *  # noqa: F401,F403


def clouds(seed, y0, y1, color, alpha=0.6, stretch=4.0, cover=0.55):
    """Horizontal streaky clouds between y0 and y1."""
    n = fbm(int(W / stretch), H, 3, 5, seed)
    from PIL import Image as _I
    n = np.asarray(_I.fromarray(n, "F").resize((W, H), _I.BICUBIC), np.float32)
    band = smoothstep(y0 - 60, y0 + 40, Y) * (1 - smoothstep(y1 - 40, y1 + 60, Y))
    m = smoothstep(cover, cover + 0.18, n) * band
    return m, color, alpha


# ======================================================================= LAKES

def osorno():
    img = gradient_v([(0, "#24365f"), (0.28, "#5a6f9e"), (0.5, "#d9a7a3"), (0.62, "#f2b98e"), (0.66, "#f6c79a"), (1, "#f6c79a")])
    # pink evening clouds
    m, _, _ = clouds(3, 90, 330, None, cover=0.52)
    cloud_col = mix(hexc("#f4a6a0"), hexc("#ffd3b0"), smoothstep(80, 340, Y)[..., None])
    img = img * (1 - m[..., None] * 0.75) + cloud_col * m[..., None] * 0.75
    horizon = 528

    # Calbuco (right): broad, broken summit
    cal = cone(1070, 330, 560, 300, roundness=110, seed=21, rough=22)
    cm = coverage_below(cal)
    img = fill(img, cm, "#666b90")
    cal_snow = cm * coverage_above(cal + 26 + (fbm1d(W, 40, 3, 22) - 0.5) * 30)
    img = fill(img, cal_snow, "#e7c3cf", 0.85)

    # Osorno cone
    prof = cone(640, 205, horizon, 360, roundness=26, seed=4, rough=3)
    body = coverage_below(prof)
    # lower slopes: dark forest/rock, lighter (hazier) towards the base
    slope_col = mix(hexc("#4f5378"), hexc("#6d6f93"), smoothstep(380, horizon, Y)[..., None])
    img = img * (1 - body[..., None]) + slope_col * body[..., None]
    # snow cap: ragged lower edge with snow tongues running down the gullies
    edge = 350 + np.abs(X[0] - 640) * 0.12 + (fbm1d(W, 8, 3, 6) - 0.5) * 40 + (fbm1d(W, 60, 3, 16) - 0.5) * 34
    snow = coverage_above(edge)
    rng_t = np.random.default_rng(17)
    for _ in range(7):
        xi = 430 + rng_t.random() * 420
        di = 15 + rng_t.random() * 55
        wi = 2 + rng_t.random() * 3.5
        e = float(edge[int(xi)])
        tri = (Y > e - 2) & (np.abs(X - xi) < wi * np.clip(1 - (Y - e) / di, 0, 1))
        snow = np.maximum(snow, tri.astype(np.float32))
    snow = blur(snow, 0.8) * body
    vgul = np.asarray(Image.fromarray(fbm(W, H // 10, 50, 3, 8), "F").resize((W, H), Image.BICUBIC), np.float32)
    lit = mix(hexc("#ffe2d8"), hexc("#e99aaa"), smoothstep(560, 820, X)[..., None])
    lit = mix(lit, hexc("#c98aa6"), smoothstep(250, 470, Y)[..., None] * 0.35)
    snow_col = lit * (0.95 + 0.07 * vgul[..., None])
    img = img * (1 - snow[..., None]) + snow_col * snow[..., None]

    # forested shore hills
    for i, (base, amp, col) in enumerate([(505, 40, "#3d4a6d"), (522, 26, "#2d3956")]):
        img = fill(img, coverage_below(ridge(base, amp, 8, 6, 30 + i)), col)
    img = fill(img, coverage_below(np.full(W, horizon, np.float32)), "#2d3956")

    # warm haze at the horizon
    img = img + radial(600, horizon, 900, 0.08)[..., None] * hexc("#f6b48a") * 0.25
    img = reflect(img, horizon, ripple=2.0, blur_sigma=2.5, darken=0.82, seed=2)
    # long horizontal sheen lines on the water
    sheen = np.asarray(Image.fromarray(fbm(W // 25, H, 6, 3, 33), "F").resize((W, H), Image.BICUBIC), np.float32)
    lines = smoothstep(0.6, 0.7, sheen) * smoothstep(horizon + 5, H, Y)
    img = fill(img, lines, "#f8cfb0", 0.16)
    img = fill(img, smoothstep(0.35, 0.25, sheen) * smoothstep(horizon + 5, H, Y), "#1c2440", 0.12)

    # reeds in the foreground corners
    def reeds(d, s):
        rng = np.random.default_rng(3)
        for _ in range(160):
            side = rng.random() < 0.6
            x = rng.random() * 260 if side else W - rng.random() * 200
            yb = H + 5
            h = 60 + rng.random() * 170 * (1 - abs(x - (0 if side else W)) / 300)
            bend = (rng.random() - 0.3) * 40
            d.line([(x * s, yb * s), ((x + bend * 0.5) * s, (yb - h * 0.6) * s), ((x + bend) * s, (yb - h) * s)], fill=255, width=max(1, int(2.2 * s)))
    img = fill(img, draw_mask(reeds), "#161827", 0.95)
    return img


# ======================================================================= ATACAMA

def valle_luna():
    img = gradient_v([(0, "#3b4f86"), (0.35, "#c58b8a"), (0.55, "#f3b37a"), (0.62, "#ffd29a"), (1, "#ffd29a")])
    img = glow(img, 170, 470, 200, "#ffb35a", 0.35)
    img = glow(img, 170, 470, 40, "#fff0c8", 0.6)
    horizon = 470
    # Licancabur & Juriques far away, glowing pink
    lic = cone(930, 300, horizon + 20, 300, roundness=30, seed=2, rough=5)
    jur = cone(1180, 340, horizon + 20, 260, roundness=60, seed=3, rough=5)
    far = np.minimum(lic, jur)
    fm = coverage_below(far)
    glow_col = mix(hexc("#e7a2a2"), hexc("#a77a96"), smoothstep(320, 480, Y)[..., None])
    img = img * (1 - fm[..., None]) + glow_col * fm[..., None]
    # Cordillera de la Sal ridges, warm
    layers = [
        (520, 70, "#c9805f", 5, 11, True),
        (585, 110, "#b0603f", 4, 12, True),
        (680, 160, "#8f4730", 3, 13, True),
    ]
    for base, amp, col, cells, seed, sharp in layers:
        r = ridge(base, amp, cells, 7, seed, sharp)
        m = coverage_below(r)
        # lit edges facing the setting sun (left)
        img = fill(img, m, col)
        facing = np.clip(-np.gradient(r) * 0.8, 0, 1)[None, :]  # slopes facing the sun (left)
        rim = m * (1 - coverage_below(r + 6)) * facing
        img = fill(img, rim, "#ffc58a", 0.6)
        lit = m * smoothstep(r[None, :] + 50, r[None, :], Y) * facing
        img = fill(img, lit, "#e8955f", 0.35)
        # erosion texture
        tex = fbm(W, H, 50, 4, seed + 50)
        img = img * (1 - m[..., None] * 0.18 * (tex[..., None] - 0.5))
    # great dune (sand), smooth
    dune = 700 - 160 * np.exp(-((X[0] - 380) / 420) ** 2) + (fbm1d(W, 3, 3, 4) - 0.5) * 20
    dm = coverage_below(dune)
    shade = smoothstep(200, 700, X)[..., None]
    sand = mix(hexc("#f2b27a"), hexc("#9a5a46"), shade)
    img = img * (1 - dm[..., None]) + sand * dm[..., None]
    # ripples on the dune
    rip = (np.sin((Y * 0.9 + X * 0.05 + (fbm(seed=9, cells=6) * 60)) * 0.32) > 0.93) * dm * smoothstep(560, 800, Y)
    img = fill(img, blur(rip.astype(np.float32), 0.6), "#7d4637", 0.12)
    # tiny people on the crest for scale
    def people(d, s):
        for i, x in enumerate([520, 548]):
            yb = float(dune[int(x)]) + 1
            d.rectangle([x * s, (yb - 16) * s, (x + 4) * s, yb * s], fill=255)
            d.ellipse([(x - 0.5) * s, (yb - 22) * s, (x + 4.5) * s, (yb - 16) * s], fill=255)
    img = fill(img, draw_mask(people), "#2b1716")
    return img


def milky_way():
    img = gradient_v([(0, "#04060d"), (0.6, "#0a1022"), (0.8, "#1a2440"), (0.86, "#2c3350"), (1, "#2c3350")])
    # Milky Way band diagonal
    ang = np.deg2rad(-58)
    u = (X - 700) * np.cos(ang) + (Y - 420) * np.sin(ang)
    v = -(X - 700) * np.sin(ang) + (Y - 420) * np.cos(ang)
    band = np.exp(-(v / 150) ** 2)
    neb = fbm(seed=40, cells=6, octaves=6)
    dust = smoothstep(0.52, 0.7, fbm(seed=41, cells=10, octaves=5)) * np.exp(-(v / 60) ** 2)
    glowb = band * (0.45 + 0.9 * neb) * (1 - 0.75 * dust)
    mw_col = mix(hexc("#5c6aa0"), hexc("#e8d6c0"), np.clip(glowb, 0, 1)[..., None])
    img = img + mw_col * glowb[..., None] * 0.42
    # Carina nebula pink knot and LMC/SMC smudges
    img = glow(img, 470, 300, 22, "#ff7aa8", 0.35)
    img = glow(img, 260, 170, 55, "#c9c9e6", 0.16, 0.7)
    img = glow(img, 180, 330, 28, "#c9c9e6", 0.12, 0.7)
    img = stars(img, 5200, 11, max_y=H, bright=1.0, band=np.clip(band * 0.9, 0, 1))
    # airglow near horizon
    img = img + smoothstep(350, 640, Y)[..., None] * hexc("#2c4a3c") * 0.25
    # Licancabur silhouette & rocky foreground
    lic = cone(820, 470, 650, 260, roundness=25, seed=7, rough=3)
    img = fill(img, coverage_below(lic), "#0d1220")
    fg = ridge(700, 120, 4, 7, 17, sharp=True)
    img = fill(img, coverage_below(fg), "#07090f")
    # person with headlamp for scale
    def person(d, s):
        x, yb = 300, float(fg[300]) + 2
        d.rectangle([x * s, (yb - 30) * s, (x + 8) * s, yb * s], fill=255)
        d.ellipse([(x - 1) * s, (yb - 40) * s, (x + 9) * s, (yb - 29) * s], fill=255)
    img = fill(img, draw_mask(person), "#06070c")
    img = glow(img, 304, float(fg[300]) - 34, 5, "#ff4030", 0.9)
    img = glow(img, 304, float(fg[300]) - 34, 30, "#ff4030", 0.12)
    return img


def el_tatio():
    img = gradient_v([(0, "#1d2b52"), (0.3, "#3d4f80"), (0.48, "#b98a99"), (0.56, "#f6b27a"), (1, "#f6b27a")])
    sun_x, sun_y = 860, 455
    img = glow(img, sun_x, sun_y, 160, "#ffb060", 0.55)
    img = glow(img, sun_x, sun_y, 26, "#fff4d8", 1.1)
    horizon = 470
    mtn = np.minimum(ridge(440, 110, 3, 6, 5), cone(300, 300, 470, 260, 40, 3, 5))
    mm = coverage_below(mtn)
    img = fill(img, mm, "#2f3253")
    img = fill(img, mm * smoothstep(mtn[None, :] + 30, mtn[None, :], Y), "#b7a0b8", 0.5)
    ground = ridge(horizon + 22, 14, 6, 5, 6)
    gm = coverage_below(ground)
    # mineral ground: horizontal (perspective) patches of ochre, rust and white crust
    pat = np.asarray(Image.fromarray(fbm(W // 4, H, 6, 5, 60), "F").resize((W, H), Image.BICUBIC), np.float32)
    pat2 = np.asarray(Image.fromarray(fbm(W // 6, H, 14, 4, 62), "F").resize((W, H), Image.BICUBIC), np.float32)
    base_col = mix(hexc("#3b2f3a"), hexc("#6b4a3c"), smoothstep(0.35, 0.65, pat)[..., None])
    base_col = mix(base_col, hexc("#b9a89a"), (smoothstep(0.6, 0.7, pat2) * 0.7)[..., None])
    depth = smoothstep(horizon, H, Y)[..., None]
    base_col = base_col * (0.75 + 0.35 * depth)
    img = img * (1 - gm[..., None]) + base_col * gm[..., None]
    # puddles reflecting the orange sky
    pud = smoothstep(0.73, 0.76, np.asarray(Image.fromarray(fbm(W // 10, H, 16, 3, 61), "F").resize((W, H), Image.BICUBIC), np.float32)) * gm * smoothstep(520, 600, Y)
    img = fill(img, pud, "#f2a46e", 0.75)
    # steam columns, backlit by the low sun
    steam = np.zeros((H, W), np.float32)
    for i, (cx, base, hgt, wid) in enumerate([(250, 690, 520, 60), (520, 590, 330, 40), (930, 640, 470, 70), (1110, 560, 220, 26), (720, 545, 210, 24), (80, 560, 200, 22)]):
        n = fbm(seed=70 + i, cells=8, octaves=5)
        t = np.clip((base - Y) / hgt, 0, 1)
        widen = wid * (0.5 + 2.0 * t)
        sway = (fbm1d(H, 3, 3, 80 + i)[:, None] - 0.5) * 160 * t
        m = np.exp(-((X - cx - sway) / widen) ** 2) * (Y < base) * (1 - t) ** 0.6
        steam += m * (0.35 + 0.9 * n)
    steam = blur(np.clip(steam, 0, 1.2), 2.5)
    dist = np.sqrt((X - sun_x) ** 2 + (Y - sun_y) ** 2)
    back = np.clip(1 - dist / 650, 0, 1) ** 1.5
    scol = mix(hexc("#9aa0c4"), hexc("#ffe6c4"), back[..., None])
    img = img * (1 - steam[..., None] * 0.8) + scol * steam[..., None] * 0.85
    # visitors in silhouette for scale
    def ppl(d, s):
        for x, yb, h in [(170, 735, 80), (196, 738, 72), (650, 610, 40), (1000, 600, 34), (1016, 602, 31)]:
            d.rounded_rectangle([x * s, (yb - h) * s, (x + h * 0.26) * s, yb * s], radius=int(h * 0.1 * s), fill=255)
            d.ellipse([(x - 0.5) * s, (yb - h - h * 0.22) * s, (x + h * 0.26) * s, (yb - h + 2) * s], fill=255)
    img = fill(img, draw_mask(ppl), "#15131f")
    return img


def _flamingo(d, s, x, y, sc, head_down=False, mirror=1):
    m = mirror
    P = lambda px, py: ((x + px * sc * m) * s, (y + py * sc) * s)
    # body (tear-drop) and tail
    d.ellipse([P(-24, -13)[0] if m > 0 else P(24, -13)[0], P(0, -13)[1], P(22, 9)[0] if m > 0 else P(-22, 9)[0], P(0, 9)[1]], fill=255)
    d.polygon([P(-18, -8), P(-36, -2), P(-16, 6)], fill=255)
    if head_down:
        pts = [(16, -6), (28, -26), (36, -8), (34, 24), (30, 40)]
    else:
        pts = [(16, -6), (26, -34), (16, -56), (24, -76)]
    d.line([P(px, py) for px, py in pts], fill=255, width=max(1, int(5 * sc * s)), joint="curve")
    hx, hy = pts[-1]
    d.ellipse([P(hx - 6, hy - 5)[0] if m > 0 else P(hx + 6, hy - 5)[0], P(0, hy - 5)[1], P(hx + 7, hy + 5)[0] if m > 0 else P(hx - 7, hy + 5)[0], P(0, hy + 5)[1]], fill=255)
    # beak
    d.polygon([P(hx + 5, hy - 2), P(hx + 14, hy + 3), P(hx + 4, hy + 4)], fill=255)
    # legs
    for dx in (-2, 6):
        d.line([P(dx, 6), P(dx + 1, 66)], fill=255, width=max(1, int(2 * sc * s)))


def flamingos():
    img = gradient_v([(0, "#4f6fa8"), (0.3, "#c9a2b4"), (0.42, "#ffcf98"), (0.47, "#ffe1b4"), (1, "#ffe1b4")])
    img = glow(img, 980, 380, 220, "#ffc27a", 0.35)
    horizon = 380
    vol = np.minimum.reduce([cone(330, 205, horizon, 300, 18, 5, 5), cone(700, 245, horizon, 260, 22, 6, 5), cone(1010, 280, horizon, 240, 30, 7, 5)])
    vm = coverage_below(vol)
    vcol = mix(hexc("#8d7896"), hexc("#b99aa6"), smoothstep(250, horizon, Y)[..., None])
    img = img * (1 - vm[..., None]) + vcol * vm[..., None]
    ravines = np.asarray(Image.fromarray(fbm(W, H // 12, 60, 2, 44), "F").resize((W, H), Image.BICUBIC), np.float32)
    img = img * (1 - vm[..., None] * 0.12 * (ravines[..., None] - 0.5))
    caps = vm * coverage_above(285 + (fbm1d(W, 30, 4, 8) - 0.5) * 30)
    img = fill(img, caps, "#f3e3e8", 0.9)
    img = fill(img, coverage_below(ridge(horizon + 2, 6, 14, 4, 3)), "#c79c86")
    img = reflect(img, horizon + 4, ripple=0.8, blur_sigma=1.0, darken=0.93, seed=4)
    # salt crust: thin horizontal islands in perspective
    sc_noise = np.asarray(Image.fromarray(fbm(W // 10, H, 10, 5, 90), "F").resize((W, H), Image.BICUBIC), np.float32)
    salt = smoothstep(0.6, 0.63, sc_noise) * smoothstep(horizon + 10, horizon + 60, Y)
    img = fill(img, salt, "#f2e8dc", 0.95)
    img = fill(img, salt * smoothstep(0.66, 0.7, sc_noise), "#d8c3b0", 0.6)
    img = fill(img, blur(salt, 2) * (1 - salt), "#b99a88", 0.25)

    flock = [(250, 560, 1.5, False, 1), (360, 600, 1.7, True, 1), (520, 520, 1.1, False, -1), (760, 640, 2.2, True, 1), (900, 520, 1.1, False, 1), (1010, 560, 1.3, True, -1), (1120, 500, 0.8, False, 1), (640, 480, 0.75, True, -1)]
    for x, y, sc, hd, mr in flock:
        m = draw_mask(lambda d, s: _flamingo(d, s, x, y, sc, hd, mr))
        feet = int(y + 66 * sc)
        rm = np.zeros_like(m)
        span = min(feet, H - feet)
        rm[feet:feet + span] = m[feet - span:feet][::-1]
        rm = blur_x(rm, 1.5)
        img = fill(img, rm, "#e48a99", 0.5)
        col = mix(hexc("#f07c95"), hexc("#ffb3a8"), smoothstep(y - 70 * sc, y + 10 * sc, Y)[..., None])
        img = img * (1 - m[..., None]) + col * m[..., None]
        # darker wing coverts and the thin black flight-feather line along the back
        wing = draw_mask(lambda d, s: d.ellipse([(x - 20 * sc) * s, (y - 10 * sc) * s, (x + 10 * sc) * s, (y + 2 * sc) * s], fill=255))
        img = fill(img, wing * m, "#e0607e", 0.55)
        line = draw_mask(lambda d, s: d.line([((x - 30 * sc * mr) * s, (y - 1 * sc) * s), ((x - 6 * sc * mr) * s, (y + 4 * sc) * s)], fill=255, width=max(1, int(2.2 * sc * s))))
        img = fill(img, line, "#24161c", 0.85)
    return img


def altiplano():
    img = gradient_v([(0, "#0c2a78"), (0.4, "#2f63b8"), (0.55, "#7fa9dc"), (1, "#7fa9dc")])
    horizon = 430
    left = np.minimum(cone(260, 300, horizon, 330, 60, 8, 6), ridge(420, 40, 4, 5, 8))
    mig = cone(800, 190, horizon, 420, 80, 3, 6)
    for prof, c1, c2 in [(left, "#8d6f6a", "#a88673"), (mig, "#8a5a50", "#b07a5c")]:
        m = coverage_below(prof)
        col = mix(hexc(c1), hexc(c2), smoothstep(250, horizon, Y)[..., None])
        img = img * (1 - m[..., None]) + col * m[..., None]
        strata = np.asarray(Image.fromarray(fbm(W, H // 14, 50, 3, 31), "F").resize((W, H), Image.BICUBIC), np.float32)
        img = img * (1 - m[..., None] * 0.22 * (strata[..., None] - 0.5))
        # shadow side (right) of each mountain
    img = img * (1 - coverage_below(mig)[..., None] * 0.22 * smoothstep(800, 1150, X)[..., None])
    snow_edge = 255 + np.abs(X[0] - 800) * 0.1 + (fbm1d(W, 20, 4, 3) - 0.5) * 40
    snow = coverage_below(mig) * coverage_above(snow_edge)
    img = fill(img, snow, "#f7f5f4", 0.95)
    img = fill(img, snow * smoothstep(820, 960, X), "#c9cfe4", 0.5)
    # lake with reflection and a white salt rim
    img = reflect(img, horizon, ripple=1.2, blur_sigma=1.4, darken=0.75, seed=5)
    shore = 560 + 40 * np.sin(X[0] / 260) + (fbm1d(W, 6, 4, 12) - 0.5) * 20
    water = coverage_below(np.full(W, horizon, np.float32)) * (1 - coverage_below(shore))
    deep = mix(hexc("#0b3c94"), hexc("#1c8fb0"), smoothstep(shore[None, :] - 70, shore[None, :], Y)[..., None])
    img = img * (1 - water[..., None] * 0.6) + deep * water[..., None] * 0.6
    rim = coverage_below(shore - 7) * (1 - coverage_below(shore))
    img = fill(img, rim, "#f4f0e6", 0.95)
    # golden grass land
    gm = coverage_below(shore)
    ground = mix(hexc("#b88f45"), hexc("#7d5f34"), smoothstep(560, 800, Y)[..., None])
    img = img * (1 - gm[..., None]) + ground * gm[..., None]
    rng = np.random.default_rng(9)
    tufts = sorted([(rng.random() * W, 580 + rng.random() ** 0.7 * 240) for _ in range(120)], key=lambda t: t[1])
    for tx, ty in tufts:
        size = 0.25 + (ty - 560) / 240 * 1.2
        def tuft(d, s, tx=tx, ty=ty, size=size):
            r = np.random.default_rng(int(tx * 13 + ty))
            for _ in range(28):
                a = -np.pi / 2 + (r.random() - 0.5) * 1.8
                L = (24 + r.random() * 30) * size
                d.line([(tx * s, ty * s), ((tx + np.cos(a) * L) * s, (ty + np.sin(a) * L) * s)], fill=255, width=max(1, int(1.6 * s * max(0.6, size))))
        m = draw_mask(tuft, supersample=2)
        img = fill(img, m, "#e9c46f", 0.9)
        img = fill(img, m * smoothstep(ty - 40 * size, ty, Y), "#8a6532", 0.45)
    return img


# ======================================================================= SANTIAGO & CENTRAL

def santiago():
    img = gradient_v([(0, "#16224a"), (0.3, "#34457c"), (0.5, "#8c7fa6"), (0.6, "#d9a090"), (1, "#d9a090")])
    # Andes: dark body, crisp snowy crest catching the last pink light
    andes = ridge(300, 150, 4, 7, 2, sharp=True)
    am = coverage_below(andes)
    body_col = mix(hexc("#3f4675"), hexc("#5b5f8c"), smoothstep(200, 470, Y)[..., None])
    img = img * (1 - am[..., None]) + body_col * am[..., None]
    snow_line = andes + 35 + (fbm1d(W, 50, 3, 9) - 0.5) * 40
    snow = am * coverage_above(snow_line)
    img = fill(img, snow, "#f6c1c6", 0.9)
    img = fill(img, snow * smoothstep(andes[None, :] + 4, andes[None, :] + 30, Y), "#d79aac", 0.4)
    foot = ridge(455, 55, 6, 6, 3)
    img = fill(img, coverage_below(foot), "#2f3459")
    # warm smog layer over the city
    img = img + (smoothstep(420, 500, Y) * (1 - smoothstep(520, 600, Y)))[..., None] * hexc("#d08a70") * 0.25
    city_top = 520
    rng = np.random.default_rng(7)
    bmask = np.zeros((H, W), np.float32)
    lights = np.zeros((H, W), np.float32)
    def add_building(x0, x1, top):
        nonlocal bmask, lights
        bmask[int(top):, int(x0):int(x1)] = 1
        # window grid, some lit
        for wy in range(int(top) + 6, H, 9):
            for wx in range(int(x0) + 3, int(x1) - 3, 7):
                if rng.random() < 0.38:
                    lights[wy:wy + 3, wx:wx + 3] = 0.6 + rng.random() * 0.4
    x = 0
    while x < W:
        w = int(rng.integers(16, 48))
        h = int(rng.integers(10, 70))
        add_building(x, min(W, x + w), city_top + 40 - h)
        x += w + int(rng.integers(1, 6))
    for bx, bw, bh in [(640, 30, 150), (676, 22, 115), (705, 18, 95), (600, 26, 105), (742, 34, 75), (820, 24, 90), (420, 28, 85)]:
        add_building(bx, bx + bw, city_top + 40 - bh)
    # the tall glass tower
    tower = draw_mask(lambda d, s: d.polygon([(556 * s, (city_top + 40) * s), (568 * s, (city_top - 250) * s), (590 * s, (city_top - 256) * s), (602 * s, (city_top + 40) * s)], fill=255))
    bmask = np.maximum(bmask, tower)
    tower_lines = tower * (np.sin(Y * 0.7) > 0.6)
    img = fill(img, bmask, "#171a30")
    img = fill(img, tower, "#252b4e", 0.9)
    img = fill(img, tower_lines, "#6d7bb3", 0.35)
    lights = lights * bmask * (1 - tower)
    warm = mix(hexc("#ffd27a"), hexc("#fff3d6"), rng.random((H, W)).astype(np.float32)[..., None] * 0.6)
    img = img * (1 - lights[..., None]) + warm * lights[..., None]
    img = img + blur(lights, 3)[..., None] * hexc("#ffb050") * 0.6
    # avenue light trails curving through the city
    trail = draw_mask(lambda d, s: d.line([(0, 740 * s), (300 * s, 700 * s), (700 * s, 660 * s), (1200 * s, 640 * s)], fill=255, width=int(3 * s)))
    img = img + blur(trail, 2)[..., None] * hexc("#ff8a3a") * 1.2 + trail[..., None] * hexc("#fff0c0") * 0.6
    # dark foreground hill (Cerro San Cristóbal) with tree tops
    fg = ridge(760, 50, 12, 6, 40)
    img = fill(img, coverage_below(fg), "#0b0d18")
    return img


def valparaiso():
    img = gradient_v([(0, "#5c9fd6"), (0.25, "#a9d0ee"), (1, "#a9d0ee")])
    horizon = 190
    img = fill(img, coverage_below(np.full(W, horizon, np.float32)), "#2d6f9e")
    img = img + smoothstep(horizon, 330, Y)[..., None] * hexc("#3a86b5") * 0.15
    def ships(d, s):
        for x, y, w in [(120, 196, 70), (330, 202, 110), (520, 194, 50)]:
            d.rectangle([x * s, (y - 7) * s, (x + w) * s, y * s], fill=255)
            d.rectangle([(x + w * 0.65) * s, (y - 16) * s, (x + w * 0.8) * s, (y - 7) * s], fill=255)
    img = fill(img, draw_mask(ships), "#2f3843")
    # hillside rising from bottom-left to top-right, full of houses
    hill = 330 - (X[0] / W) * 260 + (fbm1d(W, 4, 4, 2) - 0.5) * 40
    hm = coverage_below(hill)
    img = fill(img, hm, "#6f7d5c")
    palette = ["#e4572e", "#f3c13a", "#2e86c1", "#4cb944", "#e75a7c", "#f4a259", "#7b68ee", "#f9f4ef", "#17bebb", "#ff7f11", "#c2185b", "#ffd166"]
    rng = np.random.default_rng(12)
    canvas = Image.new("RGB", (W * 2, H * 2))
    mask = Image.new("L", (W * 2, H * 2), 0)
    d = ImageDraw.Draw(canvas)
    dm = ImageDraw.Draw(mask)
    rows = []
    for row in range(26):
        yb = 140 + row * 28
        rows.append(yb)
    for yb in rows:
        x = -30 + int(rng.integers(0, 20))
        while x < W + 30:
            w = int(rng.integers(30, 70))
            h = int(rng.integers(30, 52))
            mid = min(W - 1, max(0, x + w // 2))
            if yb - h > hill[mid] - 6:
                col = tuple(int(c * 255) for c in hexc(palette[rng.integers(0, len(palette))]))
                shade = tuple(int(c * 0.72) for c in col)
                roof = (int(110 + rng.integers(0, 50)), int(60 + rng.integers(0, 30)), int(50 + rng.integers(0, 20)))
                S = 2
                d.rectangle([x * S, (yb - h) * S, (x + w) * S, (yb + 8) * S], fill=col)
                d.rectangle([(x + w * 0.78) * S, (yb - h) * S, (x + w) * S, (yb + 8) * S], fill=shade)  # shaded side
                d.polygon([((x - 3) * S, (yb - h) * S), ((x + w * 0.5) * S, (yb - h - 9) * S), ((x + w + 3) * S, (yb - h) * S)], fill=roof)
                for wx in range(x + 6, x + int(w * 0.75) - 4, 11):
                    for wy in (yb - h + 8, yb - h + 24):
                        if wy + 9 < yb:
                            d.rectangle([wx * S, wy * S, (wx + 5) * S, (wy + 9) * S], fill=(35, 45, 60))
                dm.rectangle([x * S, (yb - h - 9) * S, (x + w) * S, (yb + 8) * S], fill=255)
            x += w + int(rng.integers(-3, 5))
    houses = np.asarray(canvas.resize((W, H), Image.LANCZOS), np.float32) / 255
    hmask = np.asarray(mask.resize((W, H), Image.LANCZOS), np.float32) / 255 * hm
    img = img * (1 - hmask[..., None]) + houses * hmask[..., None]
    # ascensor (funicular) track and car
    track = draw_mask(lambda d, s: d.line([(260 * s, 800 * s), (470 * s, 330 * s)], fill=255, width=int(7 * s)))
    img = fill(img, track, "#3b2f2a")
    car = draw_mask(lambda d, s: d.polygon([(340 * s, 600 * s), (380 * s, 590 * s), (392 * s, 630 * s), (352 * s, 640 * s)], fill=255))
    img = fill(img, car, "#c62828")
    # overhead cables
    cables = draw_mask(lambda d, s: [d.line([(0, (560 + k * 14) * s), (W * s, (430 + k * 22) * s)], fill=255, width=max(1, int(1.2 * s))) for k in range(3)])
    img = fill(img, cables, "#1d1d22", 0.7)
    return img


def _condor(d, s, cx, cy, span, tilt=0.0):
    k = span / 340
    c, sn = np.cos(tilt), np.sin(tilt)
    T = lambda x, y: ((cx + (x * c - y * sn) * k) * s, (cy + (x * sn + y * c) * k) * s)
    # wings: long, broad, slightly raised tips
    left = [(-10, -6), (-60, -14), (-110, -20), (-140, -22), (-150, -16), (-140, -2), (-110, 4), (-70, 10), (-30, 16)]
    right = [(-x, y) for x, y in reversed(left)]
    d.polygon([T(x, y) for x, y in left + [(-14, 26), (0, 40), (14, 26)] + right], fill=255)
    # separated primary feathers ('fingers')
    for side in (-1, 1):
        for f in range(7):
            base = (side * (138 + f * 2), -18 + f * 3.2)
            tip = (side * (168 + f * 1.5 - f * f * 0.4), -26 + f * 6)
            mid = (side * (150 + f * 2), -20 + f * 4.5)
            d.polygon([T(*base), T(mid[0], mid[1] - 2.2), T(*tip), T(mid[0], mid[1] + 2.2)], fill=255)
    # tail and head
    d.polygon([T(-12, 24), T(0, 46), T(12, 24)], fill=255)
    d.ellipse([T(-8, -22)[0], T(-8, -22)[1], T(8, -6)[0], T(8, -6)[1]], fill=255)


def condor():
    img = gradient_v([(0, "#2766ad"), (0.55, "#86b9e2"), (1, "#c6def0")])
    # sunlit canyon wall on the right, shaded wall on the left
    right_edge = 520 + (fbm1d(H, 5, 6, 3) - 0.5) * 220 + (np.arange(H) - 400) * -0.25
    rm = np.clip(X - right_edge[:, None] + 0.5, 0, 1)
    top = ridge(170, 110, 4, 7, 9, sharp=True)
    rm = rm * coverage_below(top)
    wall = mix(hexc("#b98a65"), hexc("#8c6248"), smoothstep(150, 800, Y)[..., None])
    img = img * (1 - rm[..., None]) + wall * rm[..., None]
    strata = np.asarray(Image.fromarray(fbm(W // 8, H, 20, 4, 4), "F").resize((W, H), Image.BICUBIC), np.float32)
    img = img * (1 - rm[..., None] * 0.35 * (strata[..., None] - 0.5))
    img = fill(img, rm * smoothstep(top[None, :] + 40, top[None, :] + 5, Y), "#f2eee9", 0.8)
    left_edge = 160 + (fbm1d(H, 4, 6, 7) - 0.5) * 160 + (np.arange(H) - 400) * 0.18
    lm = np.clip(left_edge[:, None] - X + 0.5, 0, 1) * smoothstep(200, 280, Y)
    img = fill(img, lm, "#5c4334")
    img = img * (1 - lm[..., None] * 0.3 * (strata[..., None] - 0.5))
    valley = ridge(740, 50, 5, 6, 11)
    img = fill(img, coverage_below(valley), "#55663f")
    # the condor, close and large, plus a second one far away
    big = draw_mask(lambda d, s: _condor(d, s, 560, 330, 620, -0.06))
    img = fill(img, big, "#141210")
    # white secondaries band on the upper wing and white neck ruff
    band = draw_mask(lambda d, s: [d.polygon([((560 + sd * 50) * s, 330 * s), ((560 + sd * 150) * s, 318 * s), ((560 + sd * 215) * s, 314 * s), ((560 + sd * 150) * s, 326 * s), ((560 + sd * 50) * s, 340 * s)], fill=255) for sd in (-1, 1)])
    img = fill(img, blur(band, 1.5) * big, "#d9d4cc", 0.7)
    # subtle sheen on the wings
    img = fill(img, big * smoothstep(340, 300, Y), "#3a352f", 0.5)
    ruff = draw_mask(lambda d, s: d.ellipse([548 * s, 318 * s, 572 * s, 332 * s], fill=255))
    img = fill(img, ruff, "#f6f2ea")
    head = draw_mask(lambda d, s: d.ellipse([552 * s, 302 * s, 568 * s, 318 * s], fill=255))
    img = fill(img, head, "#4a3530")
    far = draw_mask(lambda d, s: _condor(d, s, 930, 470, 120, 0.25))
    img = fill(img, far, "#2b2420", 0.9)
    return img


# ======================================================================= LAKE DISTRICT & CHILOÉ

def petrohue():
    img = gradient_v([(0, "#7fa8d6"), (0.35, "#cfe0ee"), (1, "#cfe0ee")])
    osorno = cone(820, 80, 370, 310, 22, 4, 3)
    om = coverage_below(osorno)
    img = fill(img, om, "#6f7896")
    edge = 175 + np.abs(X[0] - 820) * 0.12 + (fbm1d(W, 10, 4, 6) - 0.5) * 30 + (fbm1d(W, 50, 3, 7) - 0.5) * 24
    img = fill(img, om * coverage_above(edge), "#fbfcfe", 0.97)
    img = fill(img, om * coverage_above(edge) * smoothstep(830, 1000, X), "#c4cde2", 0.6)
    forest = ridge(375, 55, 10, 7, 5)
    fm = coverage_below(forest)
    img = fill(img, fm, "#2c5536")
    canopy = fbm(seed=6, cells=90, octaves=2)
    img = img * (1 - fm[..., None] * 0.35 * (canopy[..., None] - 0.5))
    img = fill(img, fm * smoothstep(forest[None, :] + 25, forest[None, :], Y), "#5f8a4c", 0.35)

    top = 410
    # silky turquoise river: vertical flow streaks
    flow = np.asarray(Image.fromarray(fbm(W // 2, H // 10, 50, 4, 10), "F").resize((W, H), Image.BICUBIC), np.float32)
    flow = blur(flow, 1.5)
    river = mix(hexc("#159c93"), hexc("#7fdccd"), smoothstep(0.3, 0.8, flow)[..., None] * 0.7)
    river = mix(river, hexc("#0c6f73"), smoothstep(top, H, Y)[..., None] * 0.35)
    wm = smoothstep(top - 4, top + 4, Y)
    img = img * (1 - wm[..., None]) + river * wm[..., None]
    # ledges where the water tumbles: bright white foam lines that follow the rock edge
    for i, yy in enumerate((455, 545, 650)):
        ledge = yy + (fbm1d(W, 6, 4, 50 + i) - 0.5) * 50
        below = Y - ledge[None, :]
        cascade = (below > -3) * np.exp(-np.clip(below, 0, None) / 26) * (0.55 + 0.6 * flow)
        broken = smoothstep(0.35, 0.55, fbm(seed=60 + i, cells=12, octaves=3))
        img = fill(img, np.clip(cascade * broken, 0, 1), "#effcf9", 0.85)
        img = fill(img, np.exp(-(below / 5) ** 2) * broken, "#ffffff", 0.7)
    # basalt rocks: smooth dark islands with sky-lit tops
    rocks = smoothstep(0.56, 0.585, fbm(seed=8, cells=3, octaves=4)) * smoothstep(top + 20, top + 60, Y)
    rock_col = mix(hexc("#1b1e22"), hexc("#30363c"), fbm(seed=12, cells=30, octaves=3)[..., None])
    img = img * (1 - rocks[..., None]) + rock_col * rocks[..., None]
    top_lit = rocks * (1 - np.roll(rocks, 6, axis=0))
    img = fill(img, blur(top_lit, 1.5), "#8fa4ae", 0.45)
    # big wet foreground rock bottom-left
    fg = 640 + 0.0009 * (X[0] - 0) ** 2
    fgm = coverage_below(fg) * (X < 560)
    fgm = blur(fgm, 1)
    img = fill(img, fgm, "#121518")
    img = fill(img, fgm * smoothstep(fg[None, :] + 30, fg[None, :], Y), "#6e8794", 0.45)
    return img


def palafitos():
    img = gradient_v([(0, "#9aa7b4"), (0.5, "#cfd6db"), (1, "#cfd6db")])
    hills = ridge(260, 50, 5, 6, 3)
    img = fill(img, coverage_below(hills), "#6f8a74")
    horizon = 520
    palette = ["#d64933", "#f0c330", "#2a7ab0", "#7fb069", "#e98a15", "#f2f2f2", "#8e6c8a", "#3caea3", "#d9805e"]
    rng = np.random.default_rng(4)
    x = 20
    mask = np.zeros((H, W), np.float32)
    col_img = np.zeros((H, W, 3), np.float32)
    while x < W - 40:
        w = int(rng.integers(90, 150))
        h = int(rng.integers(90, 150))
        top = horizon - 60 - h
        col = hexc(palette[rng.integers(0, len(palette))])
        def house(d, s, x=x, w=w, h=h, top=top):
            d.rectangle([x * s, top * s, (x + w) * s, (horizon - 60) * s], fill=255)
            d.polygon([((x - 6) * s, top * s), ((x + w / 2) * s, (top - 40) * s), ((x + w + 6) * s, top * s)], fill=255)
        m = draw_mask(house, supersample=2)
        col_img = col_img * (1 - m[..., None]) + col * m[..., None]
        mask = np.maximum(mask, m)
        # shingles texture
        x += w + int(rng.integers(6, 18))
    shingles = (np.sin(Y * 0.9) > 0.6) * 0.1 + (np.sin(X * 0.6 + (Y // 6) * 2) > 0.85) * 0.08
    img = img * (1 - mask[..., None]) + col_img * (1 - shingles[..., None]) * mask[..., None]
    # windows
    win = ((np.sin(X * 0.12) > 0.75) & (np.sin(Y * 0.12 + 1) > 0.7)).astype(np.float32) * mask * (Y < horizon - 70)
    img = fill(img, win, "#f7f3e8", 0.85)
    # stilts
    stilts = ((np.sin(X * 0.35) > 0.92) & (Y > horizon - 60) & (Y < horizon + 10)).astype(np.float32) * (np.max(mask, 0)[None, :] > 0)
    img = fill(img, stilts, "#3b302a", 0.95)
    img = fill(img, coverage_below(np.full(W, horizon - 60, np.float32)) * coverage_above(np.full(W, horizon, np.float32)) * (1 - stilts), "#556670", 0.0)
    img = reflect(img, horizon, ripple=2.0, blur_sigma=2.0, darken=0.75, seed=3)
    # boat
    def boat(d, s):
        d.polygon([(780 * s, 640 * s), (980 * s, 640 * s), (950 * s, 670 * s), (800 * s, 670 * s)], fill=255)
    bm = draw_mask(boat)
    img = fill(img, bm, "#2b5f8a")
    img = fill(img, bm * (Y < 648), "#f4f4f4")
    return img


def forest():
    img = gradient_v([(0, "#d6e0cf"), (0.5, "#a9bb9e"), (1, "#5f7a55")])
    rng = np.random.default_rng(2)
    for layer in range(6):
        depth = layer / 5.0                    # 0 = far, 1 = near
        trunk_col = mix(hexc("#b7c5ae"), hexc("#1a2418"), depth ** 0.8)
        leaf_col = mix(hexc("#a7bc98"), hexc("#20361d"), depth ** 0.8)
        def trees(d, s, layer=layer, depth=depth):
            for _ in range(7 + layer * 2):
                x = rng.random() * (W + 100) - 50
                w = 5 + depth * 34 + rng.random() * 8 * (1 + depth)
                bend = (rng.random() - 0.5) * 50
                pts_l = [((x + bend * (t ** 2)) * s, (H * t) * s) for t in np.linspace(0, 1, 12)]
                pts_r = [((x + w * (0.75 + 0.35 * t) + bend * (t ** 2)) * s, (H * t) * s) for t in np.linspace(1, 0, 12)]
                d.polygon(pts_l + pts_r, fill=255)
        tm = draw_mask(trees, supersample=1)
        tm = blur(tm, 2.2 * (1 - depth) + 0.3)
        img = fill(img, tm, trunk_col, 0.97)
        # moss on the near trunks
        if depth > 0.4:
            streak = np.asarray(Image.fromarray(fbm(W, H // 12, 70, 3, layer + 30), "F").resize((W, H), Image.BICUBIC), np.float32)
            moss = tm * smoothstep(0.5, 0.62, streak)
            img = fill(img, moss, "#5d8a3a", 0.55 * depth)
        if depth < 0.5:
            canopy = smoothstep(0.45, 0.65, fbm(seed=layer + 10, cells=5, octaves=3)) * smoothstep(300, 40, Y)
            img = fill(img, blur(canopy, 6), leaf_col, 0.75)
        # mist between the layers
        img = mix(img, hexc("#dfe8d8"), (0.16 * (1 - depth) * smoothstep(780, 250, Y))[..., None])
    # soft god-rays from the upper right
    ang = np.deg2rad(28)
    u = (X - 950) * np.cos(ang) + (Y + 150) * np.sin(ang)
    beams = np.zeros((H, W), np.float32)
    for c0, wdt in [(-80, 30), (20, 18), (110, 40), (230, 22)]:
        beams += np.exp(-((u - c0) / wdt) ** 2)
    beams *= smoothstep(800, 0, Y)
    img = screen(img, beams[..., None] * hexc("#fff3cc"), 0.28)
    # nalca leaves and ferns in the foreground
    def nalca(d, s):
        for cx, cy, r in [(150, 760, 120), (330, 800, 90), (1030, 770, 140), (880, 820, 80)]:
            d.ellipse([(cx - r) * s, (cy - r * 0.55) * s, (cx + r) * s, (cy + r * 0.55) * s], fill=255)
    nm = draw_mask(nalca)
    img = fill(img, nm, "#2f6a28")
    veins = nm * (np.abs(np.sin((X - 600) * 0.04 + (Y - 800) * 0.03)) < 0.06)
    img = fill(img, veins, "#8ec26a", 0.5)
    img = fill(img, nm * smoothstep(820, 690, Y), "#6aa64a", 0.35)
    def ferns(d, s):
        for _ in range(70):
            x = rng.random() * W
            yb = 690 + rng.random() * 120
            L = 50 + rng.random() * 90
            a = -np.pi / 2 + (rng.random() - 0.5) * 2.0
            pts = [((x + np.cos(a) * L * t + 24 * t * t) * s, (yb + np.sin(a) * L * t + 34 * t * t) * s) for t in np.linspace(0, 1, 12)]
            d.line(pts, fill=255, width=int(2.5 * s))
            for t in np.linspace(0.12, 0.92, 9):
                px, py = x + np.cos(a) * L * t + 24 * t * t, yb + np.sin(a) * L * t + 34 * t * t
                for side in (-1, 1):
                    d.line([(px * s, py * s), ((px + side * 15 * (1 - t)) * s, (py + 5) * s)], fill=255, width=int(2 * s))
    fm = draw_mask(ferns)
    img = fill(img, fm, "#4a8a3c", 0.95)
    return img


def penguins():
    img = gradient_v([(0, "#8fa9bf"), (0.4, "#d2dde4"), (1, "#d2dde4")])
    horizon = 300
    sea = mix(hexc("#3d6f86"), hexc("#28546b"), smoothstep(horizon, H, Y)[..., None])
    wm = coverage_below(np.full(W, horizon, np.float32))
    img = img * (1 - wm[..., None]) + sea * wm[..., None]
    waves = np.asarray(Image.fromarray(fbm(W // 12, H, 30, 3, 5), "F").resize((W, H), Image.BICUBIC), np.float32)
    img = fill(img, smoothstep(0.62, 0.7, waves) * wm, "#a8c6d2", 0.35)
    img = fill(img, smoothstep(0.38, 0.3, waves) * wm, "#1c3d50", 0.3)
    # rocky islet (Islotes de Puñihuil) with green top
    islet = 300 - 190 * np.exp(-((X[0] - 640) / 260) ** 2) + (fbm1d(W, 14, 5, 4) - 0.5) * 60
    im = coverage_below(islet) * (1 - coverage_below(np.full(W, 470, np.float32)))
    rock = np.asarray(Image.fromarray(fbm(W // 3, H, 20, 4, 8), "F").resize((W, H), Image.BICUBIC), np.float32)
    rock_col = mix(hexc("#4c4641"), hexc("#7a7068"), rock[..., None])
    rock_col = mix(rock_col, hexc("#33302d"), smoothstep(500, 900, X)[..., None] * 0.5)
    img = img * (1 - im[..., None]) + rock_col * im[..., None]
    veg = im * coverage_above(islet + 50 + (fbm1d(W, 30, 3, 6) - 0.5) * 40)
    img = fill(img, veg, "#5f7f3c", 0.92)
    img = fill(img, veg * smoothstep(0.55, 0.7, fbm(seed=7, cells=40, octaves=3)), "#86a456", 0.6)
    # surf around the base
    surf = np.exp(-((Y - 470) / 5) ** 2) * smoothstep(0.4, 0.7, fbm(seed=12, cells=12, octaves=3)) * smoothstep(300, 420, X) * (1 - smoothstep(900, 1020, X))
    img = fill(img, blur(surf, 1.5), "#e9f1f4", 0.6)
    # foreground ledge where penguins stand
    ledge = 610 + (fbm1d(W, 5, 5, 9) - 0.5) * 60
    lm = coverage_below(ledge)
    lrock = np.asarray(Image.fromarray(fbm(W // 4, H, 16, 4, 19), "F").resize((W, H), Image.BICUBIC), np.float32)
    img = img * (1 - lm[..., None]) + mix(hexc("#2d2a28"), hexc("#5a534d"), lrock[..., None]) * lm[..., None]
    img = fill(img, lm * smoothstep(ledge[None, :] + 14, ledge[None, :], Y), "#8d8378", 0.5)
    spray = np.exp(-((Y - ledge[None, :] - 4) / 5) ** 2) * smoothstep(0.45, 0.65, fbm(seed=14, cells=40, octaves=2))
    img = fill(img, spray * (X < 260), "#f4f8fa", 0.7)

    def penguin(x, yb, h, facing):
        w = h * 0.4
        def body(d, s):
            d.ellipse([(x - w / 2) * s, (yb - h) * s, (x + w / 2) * s, yb * s], fill=255)
            d.ellipse([(x - w * 0.32) * s, (yb - h * 1.2) * s, (x + w * 0.32) * s, (yb - h * 0.8) * s], fill=255)
            d.polygon([((x + facing * w * 0.25) * s, (yb - h * 1.06) * s), ((x + facing * w * 0.72) * s, (yb - h * 1.02) * s), ((x + facing * w * 0.25) * s, (yb - h * 0.97) * s)], fill=255)
            d.polygon([((x - w * 0.5) * s, (yb - h * 0.7) * s), ((x - w * 0.75) * s, (yb - h * 0.25) * s), ((x - w * 0.45) * s, (yb - h * 0.35) * s)], fill=255)
        def belly(d, s):
            d.ellipse([(x - w * 0.3 + facing * w * 0.12) * s, (yb - h * 0.78) * s, (x + w * 0.3 + facing * w * 0.12) * s, (yb - h * 0.05) * s], fill=255)
        def band(d, s):
            d.arc([(x - w * 0.34 + facing * w * 0.12) * s, (yb - h * 0.86) * s, (x + w * 0.34 + facing * w * 0.12) * s, (yb - h * 0.5) * s], 20, 160, fill=255, width=max(1, int(h * 0.05 * s)))
        def face(d, s):
            d.arc([(x - w * 0.36) * s, (yb - h * 1.22) * s, (x + w * 0.34) * s, (yb - h * 0.86) * s], 200 if facing > 0 else 230, 320 if facing > 0 else 350, fill=255, width=max(1, int(h * 0.04 * s)))
        return body, belly, band, face

    for x, h, f in [(330, 88, 1), (420, 104, -1), (520, 96, 1), (640, 70, 1), (760, 110, -1)]:
        yb = float(ledge[x]) + 8
        body, belly, band, face = penguin(x, yb, h, f)
        img = fill(img, draw_mask(body), "#15171b")
        img = fill(img, draw_mask(belly), "#f3f1eb")
        img = fill(img, draw_mask(band), "#15171b")
        img = fill(img, draw_mask(face), "#f3f1eb")
        img = fill(img, draw_mask(lambda d, s: d.ellipse([(x - h * 0.15) * s, (yb - 4) * s, (x + h * 0.15) * s, (yb + 3) * s], fill=255)), "#d9a8a0", 0.6)
    return img
