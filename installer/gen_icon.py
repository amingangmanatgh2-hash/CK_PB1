#!/usr/bin/env python3
"""CK_PB1 icon generator - pure Python (no dependencies).

Draws the CK_PB1 mark (dark rounded square, cyan K bars, amber accent dot)
pixel-by-pixel, encodes PNG frames (256/128/64/48/32/16) and wraps them into
a multi-size .ico for NSIS plus a 256px PNG for the launcher/mod icon.
"""
import struct
import zlib
import math
import os

SIZES = [256, 128, 64, 48, 32, 16]

BG = (13, 17, 27, 255)        # dark navy
BG_TOP = (24, 32, 48, 255)
CYAN = (64, 200, 255, 255)
CYAN_DIM = (42, 150, 200, 255)
AMBER = (255, 197, 66, 255)
OUTLINE = (64, 200, 255, 230)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))


def draw(size):
    px = [[(0, 0, 0, 0) for _ in range(size)] for _ in range(size)]
    s = size / 256.0  # design grid is 256
    radius = 56 * s

    def put(x, y, c):
        if 0 <= x < size and 0 <= y < size:
            if c[3] >= px[y][x][3]:
                px[y][x] = c

    # rounded background with vertical gradient + thin outline
    for y in range(size):
        for x in range(size):
            # rounded rect test
            cx = min(max(x, radius), size - radius)
            cy = min(max(y, radius), size - radius)
            d = math.hypot(x - cx, y - cy)
            if d <= radius:
                grad = y / size
                put(x, y, lerp(BG_TOP, BG, grad))
                if d > radius - max(2.0, 3 * s):
                    put(x, y, (OUTLINE[0], OUTLINE[1], OUTLINE[2], 200))

    def bar(x0, y0, w, h, color):
        for yy in range(int(y0 * s), min(size, int((y0 + h) * s))):
            for xx in range(int(x0 * s), min(size, int((x0 + w) * s))):
                put(xx, yy, color)

    def disc(cx0, cy0, r, color):
        for yy in range(size):
            for xx in range(size):
                if math.hypot(xx - cx0 * s, yy - cy0 * s) <= r * s:
                    put(xx, yy, color)

    # stylized "K" made of bars (design coords on a 256 grid)
    kx, ky, kw, kh = 88, 62, 22, 132   # vertical stroke
    bar(kx, ky, kw, kh, CYAN)
    # upper diagonal
    bar(118, 104, 20, 20, CYAN_DIM)
    bar(138, 84, 20, 20, CYAN_DIM)
    bar(158, 64, 26, 22, CYAN)
    # lower diagonal
    bar(118, 132, 20, 20, CYAN_DIM)
    bar(138, 152, 20, 20, CYAN_DIM)
    bar(158, 172, 26, 22, CYAN)
    # amber accent dot (the "PB1" node)
    disc(196, 196, 16, AMBER)
    # small cyan underline
    bar(88, 208, 92, 8, CYAN_DIM)
    return px


def png_encode(px, size):
    raw = b''
    for y in range(size):
        raw += b'\x00'
        for x in range(size):
            r, g, b, a = px[y][x]
            raw += struct.pack('4B', r, g, b, a)

    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        c += struct.pack('>I', zlib.crc32(tag + data) & 0xFFFFFFFF)
        return c

    ihdr = struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0)
    return (b'\x89PNG\r\n\x1a\n'
            + chunk(b'IHDR', ihdr)
            + chunk(b'IDAT', zlib.compress(raw, 9))
            + chunk(b'IEND', b''))


def ico_encode(images):
    # images: list of (size, png_bytes)
    out = struct.pack('<HHH', 0, 1, len(images))
    offset = 6 + 16 * len(images)
    entries = b''
    blobs = b''
    for size, data in images:
        w = 0 if size >= 256 else size
        h = 0 if size >= 256 else size
        entries += struct.pack('<BBBBHHII', w, h, 0, 0, 1, 32, len(data), offset)
        blobs += data
        offset += len(data)
    return out + entries + blobs


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    images = []
    for size in SIZES:
        px = draw(size)
        images.append((size, png_encode(px, size)))
    with open(os.path.join(here, 'appicon.ico'), 'wb') as f:
        f.write(ico_encode(images))
    big = images[0][1]
    target = os.path.join(here, '..', 'client', 'src', 'main', 'resources', 'assets', 'ckpb1')
    os.makedirs(target, exist_ok=True)
    with open(os.path.join(target, 'icon.png'), 'wb') as f:
        f.write(big)
    print('CK_PB1 icons written: appicon.ico + client icon.png (%d bytes)'
          % len(big))


if __name__ == '__main__':
    main()
