"""
Tiny procedural painting toolkit used to generate the example illustrations
in the app (no photos are used; every image is painted from code).

All images are float32 arrays of shape (H, W, 3) with values in 0..1.
"""
import numpy as np
from PIL import Image, ImageDraw

W, H = 1200, 800


# ----------------------------------------------------------------- colour helpers

def hexc(h: str) -> np.ndarray:
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) / 255.0 for i in (0, 2, 4)], np.float32)


def mix(a, b, t):
    return a + (b - a) * t


def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def grid():
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    return x, y


X, Y = grid()


# ----------------------------------------------------------------- gradients

def gradient_v(stops, h=H, w=W):
    """stops: list of (position 0..1, '#hex'). Returns H x W x 3."""
    pos = np.array([s[0] for s in stops], np.float32)
    cols = np.stack([hexc(s[1]) for s in stops])
    t = np.linspace(0, 1, h, dtype=np.float32)
    col = np.stack([np.interp(t, pos, cols[:, c]) for c in range(3)], axis=1)
    return np.broadcast_to(col[:, None, :], (h, w, 3)).copy()


def radial(cx, cy, r, aspect=1.0):
    d2 = ((X - cx) / r) ** 2 + ((Y - cy) / (r * aspect)) ** 2
    return np.exp(-d2).astype(np.float32)


def glow(img, cx, cy, r, color, strength=1.0, aspect=1.0):
    g = radial(cx, cy, r, aspect)[..., None] * strength
    return img + g * hexc(color) if isinstance(color, str) else img + g * color


def screen(img, layer, amount=1.0):
    return 1 - (1 - img) * (1 - layer * amount)


# ----------------------------------------------------------------- blur

def _box(a, r, axis):
    if r < 1:
        return a
    n = a.shape[axis]
    pad = [(0, 0)] * a.ndim
    pad[axis] = (r + 1, r)
    p = np.pad(a, pad, mode="edge")
    c = np.cumsum(p, axis=axis, dtype=np.float64)
    hi = np.take(c, np.arange(2 * r + 1, 2 * r + 1 + n), axis=axis)
    lo = np.take(c, np.arange(0, n), axis=axis)
    return ((hi - lo) / (2 * r + 1)).astype(np.float32)


def blur(a, sigma):
    """Approximate gaussian blur by 3 box blurs (fast)."""
    if sigma <= 0.3:
        return a
    r = max(1, int(round(sigma * 0.85)))
    out = a
    for _ in range(3):
        out = _box(out, r, 0)
        out = _box(out, r, 1)
    return out


def blur_x(a, sigma):
    r = max(1, int(round(sigma * 0.85)))
    out = a
    for _ in range(3):
        out = _box(out, r, 1)
    return out


# ----------------------------------------------------------------- noise

def value_noise(w, h, cells, seed):
    rng = np.random.default_rng(seed)
    gw, gh = max(2, int(cells * w / max(w, h)) + 2), max(2, int(cells * h / max(w, h)) + 2)
    g = rng.random((gh, gw)).astype(np.float32)
    im = Image.fromarray(g, "F").resize((w, h), Image.BICUBIC)
    return np.asarray(im, np.float32)


def fbm(w=W, h=H, cells=4, octaves=5, seed=0, persistence=0.5):
    total = np.zeros((h, w), np.float32)
    amp, norm = 1.0, 0.0
    for o in range(octaves):
        total += amp * value_noise(w, h, cells * 2 ** o, seed + o * 101)
        norm += amp
        amp *= persistence
    return total / norm


def fbm1d(n=W, cells=6, octaves=6, seed=0, persistence=0.5):
    rng = np.random.default_rng(seed)
    out = np.zeros(n, np.float32)
    amp, norm = 1.0, 0.0
    x = np.linspace(0, 1, n)
    for o in range(octaves):
        k = cells * 2 ** o + 2
        pts = rng.random(k)
        xp = np.linspace(0, 1, k)
        # smooth (cosine) interpolation
        idx = np.clip((x * (k - 1)).astype(int), 0, k - 2)
        t = x * (k - 1) - idx
        t = (1 - np.cos(t * np.pi)) / 2
        out += amp * (pts[idx] * (1 - t) + pts[idx + 1] * t)
        norm += amp
        amp *= persistence
    return out / norm


# ----------------------------------------------------------------- shapes

def coverage_below(curve):
    """Anti-aliased mask: 1 where pixel is below (greater y than) curve[x]."""
    return np.clip(Y - curve[None, :] + 0.5, 0, 1)


def coverage_above(curve):
    return 1 - coverage_below(curve)


def fill(img, mask, color, alpha=1.0):
    m = (mask * alpha)[..., None]
    c = hexc(color) if isinstance(color, str) else color
    return img * (1 - m) + c * m


def ridge(base, amp, cells=5, octaves=6, seed=0, sharp=False):
    n = fbm1d(W, cells, octaves, seed)
    if sharp:
        n = 1 - np.abs(n * 2 - 1)
    return base - amp * n


def cone(cx, peak_y, base_y, half_width, roundness=40.0, seed=0, rough=6.0):
    """A volcano profile: returns y(x)."""
    x = np.arange(W, dtype=np.float32)
    d = np.abs(x - cx)
    # smooth min to round the summit
    slope = (base_y - peak_y) / half_width
    y = peak_y + slope * np.sqrt(d ** 2 + roundness ** 2) - slope * roundness
    # concave flanks
    y = peak_y + (y - peak_y) * (0.75 + 0.25 * np.clip(d / half_width, 0, 1))
    y += (fbm1d(W, 20, 4, seed) - 0.5) * rough
    return np.minimum(y, base_y + 400)


def polygon_mask(points, supersample=3):
    im = Image.new("L", (W * supersample, H * supersample), 0)
    ImageDraw.Draw(im).polygon([(x * supersample, y * supersample) for x, y in points], fill=255)
    im = im.resize((W, H), Image.LANCZOS)
    return np.asarray(im, np.float32) / 255.0


def draw_mask(fn, supersample=3):
    """fn(draw, s) draws white shapes with scale factor s. Returns AA mask."""
    im = Image.new("L", (W * supersample, H * supersample), 0)
    fn(ImageDraw.Draw(im), supersample)
    im = im.resize((W, H), Image.LANCZOS)
    return np.asarray(im, np.float32) / 255.0


def reflect(img, horizon, ripple=3.0, blur_sigma=2.0, darken=0.85, seed=0):
    """Mirror everything above `horizon` into the area below it, with ripples."""
    out = img.copy()
    hz = int(horizon)
    rows = H - hz
    src_rows = np.clip(hz - 1 - np.arange(rows), 0, H - 1)
    refl = img[src_rows]
    # horizontal ripple displacement increasing towards viewer
    rng = np.random.default_rng(seed)
    wav = fbm(W, rows, 30, 3, seed) - 0.5
    strength = (np.arange(rows, dtype=np.float32) / rows)[:, None] * ripple + 0.5
    shift = (wav * strength * 6).astype(int)
    cols = np.clip(np.arange(W)[None, :] + shift, 0, W - 1)
    refl = refl[np.arange(rows)[:, None], cols]
    refl = blur_x(refl, blur_sigma) * darken
    out[hz:] = refl
    return out


# ----------------------------------------------------------------- stars

def stars(img, count, seed, max_y=H, bright=1.0, band=None):
    rng = np.random.default_rng(seed)
    xs = rng.random(count) * W
    ys = rng.random(count) * max_y
    mag = rng.power(0.25, count)  # many faint, few bright
    if band is not None:
        # more stars inside the Milky Way band
        x2 = rng.random(count * 2) * W
        y2 = rng.random(count * 2) * max_y
        keep = rng.random(count * 2) < band[np.clip(y2.astype(int), 0, H - 1), np.clip(x2.astype(int), 0, W - 1)]
        xs = np.concatenate([xs, x2[keep]])
        ys = np.concatenate([ys, y2[keep]])
        mag = np.concatenate([mag, rng.power(0.2, keep.sum())])
    layer = np.zeros((H, W), np.float32)
    xi = np.clip(xs.astype(int), 0, W - 1)
    yi = np.clip(ys.astype(int), 0, H - 1)
    np.add.at(layer, (yi, xi), mag * bright)
    soft = blur(layer, 0.9) * 5
    core = np.clip(layer, 0, 1)
    # bright stars get a halo
    halo = blur(np.where(layer > 0.75 * bright, layer, 0), 2.5) * 6
    tint = rng.random((H, W)).astype(np.float32)
    s = np.clip(core + soft + halo, 0, 1.5)[..., None]
    col = np.stack([0.9 + 0.1 * tint, 0.93 * np.ones_like(tint), 1.0 - 0.12 * tint], -1)
    return img + s * col


# ----------------------------------------------------------------- finishing

def grain(img, amount=0.02, seed=7):
    rng = np.random.default_rng(seed)
    n = rng.normal(0, amount, (H, W, 1)).astype(np.float32)
    return img + n


def vignette(img, amount=0.35, softness=0.9):
    d = np.sqrt(((X - W / 2) / (W / 2)) ** 2 + ((Y - H / 2) / (H / 2)) ** 2) / np.sqrt(2)
    v = 1 - amount * smoothstep(0.35, softness + 0.35, d)
    return img * v[..., None]


def luminance(img):
    return img[..., 0] * 0.2126 + img[..., 1] * 0.7152 + img[..., 2] * 0.0722


def save(img, path, size=None, quality=86):
    a = np.clip(img, 0, 1)
    im = Image.fromarray((a * 255 + 0.5).astype(np.uint8), "RGB")
    if size:
        im = im.resize(size, Image.LANCZOS)
    im.save(path, "WEBP", quality=quality, method=6)


# ----------------------------------------------------------------- "camera" & "edit"

def as_raw(img):
    """What an unedited RAW looks like: flat contrast, lifted blacks, muted, a touch cool."""
    lum = luminance(img)[..., None]
    out = mix(lum, img, 0.72)                      # less saturation
    out = 0.12 + out * 0.76                        # flat: lifted blacks, dull highlights
    out = out * np.array([0.97, 1.0, 1.04], np.float32)  # slightly cool / neutral
    return np.clip(out, 0, 1)


def edit(img, warmth=0.5, contrast=0.25, vibrance=0.25, shadows_tint="#2a3a5a", highlights_tint="#ffb060",
         split=0.25, fade=0.03, vig=0.3, clarity=0.0, grain_amt=0.012):
    """A warm 'finished' edit: the look the presets in the app aim for."""
    out = img.copy()
    # white balance warm shift
    out = out * np.array([1 + 0.10 * warmth, 1 + 0.02 * warmth, 1 - 0.12 * warmth], np.float32)
    # clarity (local contrast)
    if clarity > 0:
        lum = luminance(out)
        base = blur(lum, 18)
        out = out + clarity * (lum - base)[..., None]
    # S-curve contrast around mid grey
    out = np.clip(out, 0, 1)
    out = out + contrast * (out - 0.5) * (1 - np.abs(out - 0.5) * 2) * 2
    # vibrance: boost low-saturation colours more
    lum = luminance(out)[..., None]
    sat = np.max(out, -1, keepdims=True) - np.min(out, -1, keepdims=True)
    out = lum + (out - lum) * (1 + vibrance * (1 - sat))
    # split toning
    lum = luminance(np.clip(out, 0, 1))[..., None]
    sh = hexc(shadows_tint)
    hi = hexc(highlights_tint)
    w_sh = (1 - lum) ** 2 * split
    w_hi = lum ** 2 * split
    out = out * (1 - w_sh - w_hi) + (out * 0.6 + sh * 0.4) * w_sh + screen(out, hi, 0.5) * w_hi
    # faded blacks
    out = fade + out * (1 - fade)
    out = vignette(out, vig)
    out = grain(out, grain_amt)
    return np.clip(out, 0, 1)
