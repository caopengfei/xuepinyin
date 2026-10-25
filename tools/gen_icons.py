# -*- coding: utf-8 -*-
"""TV banner (640x360) 与启动图标：「暖阳写字卡」

奶油三段渐变底 + 白色大圆角「四线三格」写字卡（a / o / e 韵母按真实书写占中格、
底边贴第三线，即「写一写」模块的核心元素）+ 光芒太阳。风格基因（奶油底/暖色/太阳/
圆角/文楷）参照 threecharactorscripture 的 gen_icons.py，构图为本项目专属的拼音符号。
配色取自 ui/theme/AppTheme.kt。banner 源字体：LXGW WenKai（tools/*.ttf 已 gitignore）
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "app", "src", "main", "res")
FONT = os.path.join(HERE, "LXGWWenKai-Medium.ttf")

# 配色与 ui/theme/AppTheme.kt 一致
CREAM_TOP = (255, 248, 236)    # CreamTop
CREAM_MID = (255, 239, 216)    # CreamMid
CREAM_BOTTOM = (255, 230, 199) # CreamBottom
HONEY = (212, 149, 74)         # Honey（字母 a）
CORAL = (255, 138, 101)        # Coral = app 主色（字母 o）
LEAF = (63, 143, 107)          # HolisticGreen（字母 e）
HONEY_LIGHT = (247, 178, 103)  # 太阳
INK = (90, 70, 52)             # Ink
SUB = (138, 109, 78)           # InkSoft
WHITE = (255, 255, 255)
GRID_LINE = (90, 70, 52, 105)  # 四线格线：Ink 半透明

# 启动图标（坐标按边长 S 归一化）；写字卡 (cx, cy, w, h, rot)；太阳 (cx, cy, r)
ICON_CARD = (0.5, 0.575, 0.74, 0.50, -2.5)
ICON_SUN = (0.80, 0.17, 0.068)
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
SUPER = 4  # 超采样倍数（以最大密度 192 为基准）

# banner（@2x 1280x720 渲染后缩 640x360）：写字卡 (cx, cy, w, h, rot)；太阳 (cx, cy, r, 线宽)
BANNER_CARD = (1085, 425, 340, 230, -2.5)
BANNER_SUN = (1140, 140, 52, 22)
TITLE = "拼音乐园"
SUBTITLE = "快乐学拼音 · 一年级上册"
CARD_LETTERS = ((0.28, HONEY, "a"), (0.50, CORAL, "o"), (0.72, LEAF, "e"))


def vgrad(size, top, mid, bottom):
    w, h = size
    img = Image.new("RGB", size)
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(1, h - 1)
        if t < 0.5:
            u, a, b = t * 2, top, mid
        else:
            u, a, b = (t - 0.5) * 2, mid, bottom
        d.line([(0, y), (w, y)], fill=tuple(int(a[i] + (b[i] - a[i]) * u) for i in range(3)))
    return img


def draw_sun(d, cx, cy, r, lw):
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=HONEY_LIGHT)
    for k in range(8):
        a = k * math.pi / 4
        d.line([(cx + math.cos(a) * r * 1.45, cy + math.sin(a) * r * 1.45),
                (cx + math.cos(a) * r * 1.85, cy + math.sin(a) * r * 1.85)], fill=HONEY_LIGHT, width=lw)


def draw_grid_card(img, cx, cy, w, h, rot):
    """白色圆角写字卡：四线三格三等分 + a/o/e 韵母占中格（底边贴第三线），整卡微旋"""
    pad_w, pad_h = int(w * 1.12), int(h * 1.14)
    layer = Image.new("RGBA", (pad_w, pad_h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    x0, y0 = (pad_w - w) / 2, (pad_h - h) / 2
    d.rounded_rectangle((x0, y0, x0 + w, y0 + h), radius=h * 0.18, fill=WHITE + (255,))
    ys = [y0 + h * t for t in (0.18, 0.3933, 0.6067, 0.82)]
    lw = max(2, int(h * 0.016))
    for y in ys:
        d.line([(x0 + w * 0.07, y), (x0 + w * 0.93, y)], fill=GRID_LINE, width=lw)
    band = ys[2] - ys[1]
    probe = ImageFont.truetype(FONT, 100)
    pb = probe.getbbox("a")
    font = ImageFont.truetype(FONT, max(4, int(100 * band * 0.96 / (pb[3] - pb[1]))))
    for rx, col, letter in CARD_LETTERS:
        bb = font.getbbox(letter)
        d.text((x0 + w * rx - (bb[0] + bb[2]) / 2, ys[2] - bb[3]), letter,
               font=font, fill=col + (255,))
    layer = layer.rotate(rot, resample=Image.BICUBIC)
    img.alpha_composite(layer, (int(cx - pad_w / 2), int(cy - pad_h / 2)))


def make_launcher(preview=False):
    s = 192 * SUPER
    img = vgrad((s, s), CREAM_TOP, CREAM_MID, CREAM_BOTTOM).convert("RGBA")
    sun_layer = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    draw_sun(ImageDraw.Draw(sun_layer), int(s * ICON_SUN[0]), int(s * ICON_SUN[1]),
             int(s * ICON_SUN[2]), int(s * 0.016))
    img = Image.alpha_composite(img, sun_layer)
    draw_grid_card(img, s * ICON_CARD[0], s * ICON_CARD[1], s * ICON_CARD[2], s * ICON_CARD[3], ICON_CARD[4])
    mask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, s - 1, s - 1), radius=int(s * 0.22), fill=255)
    img = Image.composite(img, Image.new("RGBA", (s, s), (0, 0, 0, 0)), mask)
    if preview:
        img.resize((768, 768), Image.LANCZOS).save(os.path.join(HERE, "icon_preview.png"))
    for dpi, px in DENSITIES.items():
        out = os.path.join(RES, f"mipmap-{dpi}")
        os.makedirs(out, exist_ok=True)
        img.resize((px, px), Image.LANCZOS).save(os.path.join(out, "ic_launcher.png"))


def make_banner(preview=False):
    w, h = 1280, 720
    img = vgrad((w, h), CREAM_TOP, CREAM_MID, CREAM_BOTTOM).convert("RGBA")
    d = ImageDraw.Draw(img)
    d.text((90, 300), TITLE, font=ImageFont.truetype(FONT, 200), fill=INK, anchor="lm")
    d.text((95, 545), SUBTITLE, font=ImageFont.truetype(FONT, 58), fill=SUB, anchor="lm")
    draw_grid_card(img, *BANNER_CARD)
    sun_layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw_sun(ImageDraw.Draw(sun_layer), BANNER_SUN[0], BANNER_SUN[1], BANNER_SUN[2], BANNER_SUN[3])
    img = Image.alpha_composite(img, sun_layer)
    if preview:
        img.save(os.path.join(HERE, "banner_preview.png"))
    out = os.path.join(RES, "drawable-nodpi")
    os.makedirs(out, exist_ok=True)
    img.resize((640, 360), Image.LANCZOS).save(os.path.join(out, "banner.png"))


if __name__ == "__main__":
    preview = "--preview" in sys.argv
    if not os.path.exists(FONT):
        sys.exit("缺少源字体 tools/LXGWWenKai-Medium.ttf，"
                 "下载: https://github.com/lxgw/LxgwWenKai/releases")
    make_launcher(preview)
    make_banner(preview)
    print("icons OK" + ("（preview）" if preview else ""))
