"""
Synthesizes SpiderVision's UI sounds (original, no samples) into app/src/main/res/raw.
Mono 16-bit 22.05 kHz WAV, each well under 20 KB. Re-run to regenerate:
    python tools/make_sounds.py
"""
import math
import os
import random
import struct
import wave

RATE = 22050
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
random.seed(7)  # deterministic output


def write(name, samples, gain=0.8):
    peak = max(1e-9, max(abs(s) for s in samples))
    data = b"".join(struct.pack("<h", int(max(-1, min(1, s / peak * gain)) * 32767)) for s in samples)
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(data)


def env(i, n, attack=0.004, release_pow=2.0):
    t = i / RATE
    a = min(1.0, t / attack) if attack > 0 else 1.0
    return a * (1 - i / n) ** release_pow


class Bandpass:
    """RBJ biquad band-pass with a settable centre frequency."""

    def __init__(self, q):
        self.q = q
        self.x1 = self.x2 = self.y1 = self.y2 = 0.0

    def run(self, x, f):
        w = 2 * math.pi * f / RATE
        alpha = math.sin(w) / (2 * self.q)
        b0, b2 = alpha, -alpha
        a0, a1, a2 = 1 + alpha, -2 * math.cos(w), 1 - alpha
        y = (b0 * x + b2 * self.x2 - a1 * self.y1 - a2 * self.y2) / a0
        self.x2, self.x1 = self.x1, x
        self.y2, self.y1 = self.y1, y
        return y


def tick():
    # Soft "web strand" tick: a short filtered click with a tiny pitch drop.
    n = int(0.030 * RATE)
    bp = Bandpass(6)
    out = []
    for i in range(n):
        x = random.uniform(-1, 1) if i < 40 else 0.0
        f = 3200 - 1200 * i / n
        out.append(bp.run(x, f) * env(i, n, 0.0005, 3))
    return out


def thwip():
    # Web shooter: noise burst through a fast downward-sweeping band-pass, then a little snap.
    n = int(0.16 * RATE)
    bp = Bandpass(4)
    out = []
    for i in range(n):
        p = i / n
        f = 5200 * (1 - p) ** 1.6 + 500
        x = random.uniform(-1, 1)
        s = bp.run(x, f) * env(i, n, 0.003, 1.4)
        if i < int(0.012 * RATE):  # snap at the start
            s += math.sin(2 * math.pi * 1800 * i / RATE) * (1 - i / (0.012 * RATE)) * 0.6
        out.append(s)
    return out


def zip_back():
    # Back: the thwip reversed and softer, rising band-pass.
    n = int(0.12 * RATE)
    bp = Bandpass(3)
    out = []
    for i in range(n):
        p = i / n
        f = 600 + 3400 * p ** 1.3
        a = math.sin(math.pi * p) ** 1.5
        out.append(bp.run(random.uniform(-1, 1), f) * a)
    return out


def sense():
    # Spider-sense: two quick shimmering pings (perfect fifth) with fast vibrato.
    n = int(0.38 * RATE)
    out = []
    for i in range(n):
        t = i / RATE
        s = 0.0
        for start, freq in ((0.0, 1318.5), (0.07, 1975.5)):  # E6 then B6
            if t >= start:
                tt = t - start
                vib = 1 + 0.012 * math.sin(2 * math.pi * 28 * tt)
                e = math.exp(-tt * 11) * min(1, tt / 0.004)
                s += math.sin(2 * math.pi * freq * vib * tt) * e
                s += 0.35 * math.sin(2 * math.pi * freq * 2.01 * tt) * e  # shimmer partial
        out.append(s)
    return out


def buzz():
    # Can't open: low, short, soft square-ish buzz.
    n = int(0.14 * RATE)
    out = []
    for i in range(n):
        t = i / RATE
        s = math.tanh(3 * math.sin(2 * math.pi * 140 * t)) * 0.6 + 0.4 * math.sin(2 * math.pi * 210 * t)
        out.append(s * env(i, n, 0.004, 1.2))
    return out


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    write("sv_tick", tick(), 0.55)
    write("sv_thwip", thwip(), 0.8)
    write("sv_back", zip_back(), 0.6)
    write("sv_sense", sense(), 0.6)
    write("sv_buzz", buzz(), 0.55)
    for f in sorted(os.listdir(OUT)):
        print(f, os.path.getsize(os.path.join(OUT, f)), "bytes")
