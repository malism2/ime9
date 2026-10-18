# -*- coding: utf-8 -*-
"""Render the launcher icon and Google Play graphics into store/ and res/."""
from pathlib import Path

import arabic_reshaper
from bidi.algorithm import get_display
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
STORE = ROOT / "store"
FONT = Path(r"C:\Windows\Fonts")

BG = (7, 193, 96, 255)          # #07C160
INK = (17, 17, 19, 255)         # #111113
WHITE = (255, 255, 255, 255)
KEY = (255, 255, 255, 255)
SPECIAL = (181, 184, 192, 255)
KB = (216, 216, 220, 255)
MUTED = (168, 168, 176, 255)
CARD = (32, 32, 36, 255)

# 108dp adaptive-icon grid, centered inside the 66dp safe circle.
OX, OY, KW, GAP, RAD = 30, 30, 12, 6, 3


def font(name, size, index=0):
    return ImageFont.truetype(str(FONT / name), size, index=index)


def shape(text):
    if any("\u0600" <= ch <= "\u06FF" or "\u0750" <= ch <= "\u077F" for ch in text):
        return get_display(arabic_reshaper.reshape(text))
    return text


def pick_font(text, size, bold=False):
    if any("\u0600" <= ch <= "\u06FF" for ch in text):
        return font("tahomabd.ttf" if bold else "tahoma.ttf", size)
    if any("\u0400" <= ch <= "\u04FF" for ch in text):
        return font("segoeuib.ttf" if bold else "segoeui.ttf", size)
    if any("\u3040" <= ch <= "\u30FF" for ch in text) and not any("\u4E00" <= ch <= "\u9FFF" for ch in text):
        return font("YuGothB.ttc" if bold else "YuGothM.ttc", size)
    if any(
        "\u3000" <= ch <= "\u303F" or "\u4E00" <= ch <= "\u9FFF" or "\uFF00" <= ch <= "\uFFEF"
        for ch in text
    ):
        return font("msyhbd.ttc" if bold else "msyh.ttc", size)
    return font("segoeuib.ttf" if bold else "segoeui.ttf", size)


def text_size(draw, text, face):
    box = draw.textbbox((0, 0), text, font=face)
    return box[2] - box[0], box[3] - box[1], box


def draw_text(draw, text, xy, face, fill, anchor="lt"):
    shown = shape(text)
    tw, th, box = text_size(draw, shown, face)
    x, y = xy
    if anchor == "mm":
        x -= tw / 2 + box[0]
        y -= th / 2 + box[1]
    elif anchor == "lm":
        x -= box[0]
        y -= th / 2 + box[1]
    elif anchor == "rm":
        x -= tw + box[0]
        y -= th / 2 + box[1]
    draw.text((x, y), shown, font=face, fill=fill)
    return tw


def rrect(draw, box, radius, fill):
    draw.rounded_rectangle(box, radius=radius, fill=fill)


def icon_keys(canvas):
    scale = canvas / 108
    keys = []
    for row in range(3):
        for col in range(3):
            x = (OX + col * (KW + GAP)) * scale
            y = (OY + row * (KW + GAP)) * scale
            w = KW * scale
            h = KW * scale
            keys.append((x, y, x + w, y + h, row == 1 and col == 1))
    return keys


def draw_mark(draw, canvas):
    radius = max(1, RAD * canvas / 108)
    for x0, y0, x1, y1, center in icon_keys(canvas):
        rrect(draw, (x0, y0, x1, y1), radius, INK if center else WHITE)


def render_icon(size):
    image = Image.new("RGBA", (size, size), BG)
    draw_mark(ImageDraw.Draw(image), size)
    return image


def rrect_path(x, y, w, h, r):
    return (
        f"M{x + r:.1f},{y:.1f} H{x + w - r:.1f} "
        f"A{r:.1f},{r:.1f} 0 0 1 {x + w:.1f},{y + r:.1f} "
        f"V{y + h - r:.1f} A{r:.1f},{r:.1f} 0 0 1 {x + w - r:.1f},{y + h:.1f} "
        f"H{x + r:.1f} A{r:.1f},{r:.1f} 0 0 1 {x:.1f},{y + h - r:.1f} "
        f"V{y + r:.1f} A{r:.1f},{r:.1f} 0 0 1 {x + r:.1f},{y:.1f} Z"
    )


def emit_vectors():
    white, dark = [], []
    for row in range(3):
        for col in range(3):
            x = OX + col * (KW + GAP)
            y = OY + row * (KW + GAP)
            path = rrect_path(x, y, KW, KW, RAD)
            (dark if row == 1 and col == 1 else white).append(path)
    white_path = " ".join(white)
    dark_path = " ".join(dark)
    fg = f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#FFFFFF" android:pathData="{white_path}" />
    <path android:fillColor="#111113" android:pathData="{dark_path}" />
</vector>
"""
    mono = f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#000000" android:pathData="{white_path} {dark_path}" />
</vector>
"""
    full = f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#07C160" android:pathData="M0,0h108v108h-108z" />
    <path android:fillColor="#FFFFFF" android:pathData="{white_path}" />
    <path android:fillColor="#111113" android:pathData="{dark_path}" />
</vector>
"""
    (RES / "drawable" / "ic_launcher_foreground.xml").write_text(fg, encoding="utf-8")
    (RES / "drawable" / "ic_launcher_monochrome.xml").write_text(mono, encoding="utf-8")
    (RES / "drawable" / "ic_launcher.xml").write_text(full, encoding="utf-8")
    adaptive = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        folder = RES / "mipmap-anydpi-v26"
        folder.mkdir(parents=True, exist_ok=True)
        (folder / name).write_text(adaptive, encoding="utf-8")


def save_launcher_pngs():
    master = render_icon(512)
    master.save(STORE / "icon-512.png", "PNG")
    for density, size in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        folder = RES / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        image = master.resize((size, size), Image.LANCZOS)
        image.save(folder / "ic_launcher.png", "PNG")
        image.save(folder / "ic_launcher_round.png", "PNG")


def draw_backspace(draw, box, color):
    x0, y0, x1, y1 = box
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    w, h = min(36, (x1 - x0) * 0.42), min(26, (y1 - y0) * 0.28)
    left, right = cx - w / 2, cx + w / 2
    top, bot = cy - h / 2, cy + h / 2
    nose = left + w * 0.28
    draw.polygon([(nose, top), (right, top), (right, bot), (nose, bot), (left, cy)], fill=color)
    arm = w * 0.16
    draw.line((cx, cy - arm, cx + arm * 1.3, cy + arm), fill=KB, width=3)
    draw.line((cx, cy + arm, cx + arm * 1.3, cy - arm), fill=KB, width=3)


def draw_shift(draw, box, color):
    x0, y0, x1, y1 = box
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    s = min(x1 - x0, y1 - y0) * 0.22
    draw.polygon([(cx, cy - s), (cx - s, cy + s * 0.15), (cx + s, cy + s * 0.15)], fill=color)
    stem = s * 0.38
    draw.rectangle((cx - stem / 2, cy + s * 0.15, cx + stem / 2, cy + s * 0.95), fill=color)


def feature_graphic():
    image = Image.new("RGBA", (1024, 500), INK)
    draw = ImageDraw.Draw(image)
    tile = render_icon(280)
    mask = Image.new("L", (280, 280), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, 279, 279), radius=64, fill=255)
    image.paste(tile, (78, 110), mask)
    draw_text(draw, "IME9", (430, 150), font("segoeuib.ttf", 92), WHITE, "lm")
    draw_text(draw, "Seven languages, on device", (430, 250), font("segoeui.ttf", 32), BG, "lm")
    draw_text(draw, "七种语言  ·  完全离线", (430, 310), font("msyh.ttc", 30), WHITE, "lm")
    chips = ["中", "EN", "ع", "ES", "あ", "DE", "РУ"]
    x = 430
    for chip in chips:
        face = pick_font(chip, 22, bold=True)
        tw, th, box = text_size(draw, shape(chip), face)
        pad_x, chip_h = 18, 48
        width = max(56, tw + pad_x * 2)
        rrect(draw, (x, 372, x + width, 372 + chip_h), 24, WHITE)
        draw_text(draw, chip, (x + width / 2, 372 + chip_h / 2), face, INK, "mm")
        x += width + 10
    image.convert("RGB").save(STORE / "feature-graphic-1024x500.png", "PNG")


def key_label_font(label, box):
    width = box[2] - box[0]
    height = box[3] - box[1]
    size = 34 if len(label) <= 1 else 26 if len(label) <= 3 else 20
    size = int(min(size, width * 0.55, height * 0.42))
    size = max(14, size)
    return pick_font(label, size, bold=len(label) <= 2)


def draw_key(draw, box, kind, label, hint=""):
    fill = {"special": SPECIAL, "enter": BG, "space": KEY}.get(kind, KEY)
    rrect(draw, box, 10, fill)
    color = WHITE if kind == "enter" else INK
    if label == "⌫":
        draw_backspace(draw, box, color)
        return
    if label == "⇧":
        draw_shift(draw, box, color)
        return
    if hint:
        face = pick_font(label, max(18, int((box[3] - box[1]) * 0.28)), bold=True)
        hint_face = pick_font(hint, max(12, int((box[3] - box[1]) * 0.16)))
        cx, cy = (box[0] + box[2]) / 2, (box[1] + box[3]) / 2
        draw_text(draw, label, (cx, cy - 16), face, color, "mm")
        draw_text(draw, hint, (cx, cy + 22), hint_face, (60, 60, 64, 255), "mm")
        return
    face = key_label_font(label, box)
    draw_text(draw, label, ((box[0] + box[2]) / 2, (box[1] + box[3]) / 2), face, color, "mm")


def draw_key_row(draw, keys, y, height, width, rtl):
    weights = [key[2] for key in keys]
    gap = 8
    pad = 10
    inner = width - pad * 2 - gap * (len(keys) - 1)
    unit = inner / sum(weights)
    boxes = []
    if rtl:
        x = width - pad
        for kind, label, weight in keys:
            w = unit * weight
            x -= w
            boxes.append((x, y, x + w, y + height, kind, label))
            x -= gap
    else:
        x = pad
        for kind, label, weight in keys:
            w = unit * weight
            boxes.append((x, y, x + w, y + height, kind, label))
            x += w + gap
    for x0, y0, x1, y1, kind, label in boxes:
        if kind == "gap":
            continue
        hint = ""
        if "|" in label:
            label, hint = label.split("|", 1)
        draw_key(draw, (x0, y0, x1, y1), kind, label, hint)


def draw_candidates(draw, y, height, width, composing, candidates, rtl):
    rrect(draw, (0, y, width, y + height), 0, KB)
    pad = 28
    face = pick_font(composing or "字", 32)
    cand_face = pick_font(candidates[0] if candidates else "字", 36, bold=True)
    if rtl:
        x = width - pad
        if composing:
            tw = draw_text(draw, composing, (x, y + height / 2), face, BG, "rm")
            x -= tw + 28
        for index, word in enumerate(candidates):
            this = cand_face if index == 0 else pick_font(word, 34)
            color = INK
            tw = draw_text(draw, word, (x, y + height / 2), this, color, "rm")
            x -= tw + 36
            if x < 80:
                break
    else:
        x = pad
        if composing:
            tw = draw_text(draw, composing, (x, y + height / 2), face, BG, "lm")
            x += tw + 28
        for index, word in enumerate(candidates):
            this = cand_face if index == 0 else pick_font(word, 34)
            tw = draw_text(draw, word, (x, y + height / 2), this, INK, "lm")
            x += tw + 36
            if x > width - 40:
                break


def draw_t9(draw, top, width):
    pad = 10
    gap = 8
    body_h = 132 * 4 - 8
    left_w = 196
    right_w = 210
    x_left = pad
    x_center = x_left + left_w + gap
    center_w = width - pad - right_w - gap - x_center
    x_right = x_center + center_w + gap
    row_h = (body_h - gap * 3) / 4
    left = ["重输", "符", "123", "中"]
    pad_keys = [
        [("1", "，。"), ("2", "ABC"), ("3", "DEF")],
        [("4", "GHI"), ("5", "JKL"), ("6", "MNO")],
        [("7", "PQRS"), ("8", "TUV"), ("9", "WXYZ")],
        [("，", ""), ("空格", ""), ("。", "")],
    ]
    y = top
    for label in left:
        draw_key(draw, (x_left, y, x_left + left_w, y + row_h), "special", label)
        y += row_h + gap
    y = top
    key_w = (center_w - gap * 2) / 3
    for row in pad_keys:
        x = x_center
        for label, hint in row:
            kind = "space" if label == "空格" else "key"
            draw_key(draw, (x, y, x + key_w, y + row_h), kind, label, hint)
            x += key_w + gap
        y += row_h + gap
    del_h = row_h * 1.35 + gap * 0.35
    draw_key(draw, (x_right, top, x_right + right_w, top + del_h), "special", "⌫")
    draw_key(draw, (x_right, top + del_h + gap, x_right + right_w, top + body_h), "enter", "换行")


def screenshot(name, caption, subtitle, committed, composing, candidates, rows, rtl=False, t9=False):
    width, height = 1080, 1920
    image = Image.new("RGBA", (width, height), INK)
    draw = ImageDraw.Draw(image)
    draw_text(draw, "9:41", (48, 46), font("segoeuib.ttf", 28), WHITE, "lm")
    draw_text(draw, "IME9", (width - 48, 46), font("segoeuib.ttf", 28), BG, "rm")
    cap_anchor = "rm" if rtl else "lm"
    cap_x = width - 56 if rtl else 56
    draw_text(draw, caption, (cap_x, 150), pick_font(caption, 64, bold=True), WHITE, cap_anchor)
    draw_text(draw, subtitle, (cap_x, 240), pick_font(subtitle, 30), MUTED, cap_anchor)

    card = (48, 330, width - 48, 1120)
    rrect(draw, card, 28, CARD)
    body = pick_font(committed or composing or "A", 40)
    cursor_x = 88
    text_y = 400
    if rtl:
        cursor_x = width - 88
        if committed:
            tw = draw_text(draw, committed, (cursor_x, text_y), body, WHITE, "rm")
            cursor_x -= tw
        if composing:
            face = pick_font(composing, 40)
            tw = draw_text(draw, composing, (cursor_x, text_y), face, BG, "rm")
            cursor_x -= tw + 8
        draw.rectangle((cursor_x - 3, text_y - 24, cursor_x, text_y + 24), fill=BG)
    else:
        if committed:
            tw = draw_text(draw, committed, (cursor_x, text_y), body, WHITE, "lm")
            cursor_x += tw
        if composing:
            face = pick_font(composing, 40)
            tw = draw_text(draw, composing, (cursor_x, text_y), face, BG, "lm")
            underline_y = text_y + 28
            draw.rectangle((cursor_x, underline_y, cursor_x + tw, underline_y + 3), fill=BG)
            cursor_x += tw + 8
        draw.rectangle((cursor_x, text_y - 24, cursor_x + 3, text_y + 24), fill=BG)

    row_h = 132
    cand_h = 118
    row_count = 4 if t9 else len(rows)
    keyboard_h = cand_h + row_h * row_count + 16
    top = height - keyboard_h
    draw_candidates(draw, top, cand_h, width, composing, candidates, rtl)
    if t9:
        draw_t9(draw, top + cand_h + 8, width)
    else:
        y = top + cand_h
        for row in rows:
            draw_key_row(draw, row, y + 8, row_h - 12, width, rtl)
            y += row_h
    out = STORE / "screenshots" / name
    image.convert("RGB").save(out, "PNG")
    print(f"wrote {out}")


def letters(text, weight=1):
    return [("key", ch, weight) for ch in text]


def indented(text):
    return [("gap", "", 0.5)] + letters(text) + [("gap", "", 0.5)]


def bottom(lang, comma, space, period, enter):
    return [
        ("special", "123", 1.15),
        ("special", lang, 1.05),
        ("key", comma, 1.0),
        ("space", space, 2.6),
        ("key", period, 1.0),
        ("enter", enter, 1.25),
    ]


def qwerty_rows(top, mid, low, lang, comma, space, period, enter, indent=True):
    third = [("special", "⇧", 1.5)] + letters(low) + [("special", "⌫", 1.5)]
    return [
        letters(top),
        indented(mid) if indent else letters(mid),
        third,
        bottom(lang, comma, space, period, enter),
    ]


def build_screenshots():
    folder = STORE / "screenshots"
    folder.mkdir(parents=True, exist_ok=True)
    screenshot(
        "01-chinese-pinyin.png",
        "简体中文",
        "26 键拼音  ·  nihao",
        "周末见，",
        "ni'hao",
        ["你好", "你", "呢", "哪", "年"],
        qwerty_rows("qwertyuiop", "asdfghjkl", "zxcvbnm", "中", "，", "空格", "。", "换行"),
    )
    screenshot(
        "02-chinese-t9.png",
        "九宫格",
        "6426  ·  你好",
        "",
        "ni'hao",
        ["你好", "你", "好"],
        [],
        t9=True,
    )
    screenshot(
        "03-english.png",
        "English",
        "QWERTY  ·  word suggestions",
        "See you ",
        "to",
        ["to", "today", "tomorrow"],
        qwerty_rows("qwertyuiop", "asdfghjkl", "zxcvbnm", "EN", ",", "space", ".", "return"),
    )
    screenshot(
        "04-arabic.png",
        "العربية",
        "من اليمين إلى اليسار",
        "صباح ",
        "مرح",
        ["مرحبا"],
        [
            letters("ضصثقفغعهخحج"),
            letters("شسيبلاتنمكط"),
            [("special", "رمز", 1.3)] + letters("ءظذدزروىلا") + [("special", "⌫", 1.3)],
            bottom("ع", "،", "مسافة", ".", "تم"),
        ],
        rtl=True,
    )
    screenshot(
        "05-japanese.png",
        "日本語",
        "ローマ字  ·  konnichiwa",
        "",
        "こんにちわ",
        ["こんにちは", "こんにちわ"],
        qwerty_rows("qwertyuiop", "asdfghjkl", "zxcvbnm", "あ", "、", "空白", "。", "改行"),
    )
    screenshot(
        "06-spanish.png",
        "Español",
        "nino  ·  niño",
        "Hola, ",
        "nin",
        ["niño", "niña"],
        qwerty_rows("qwertyuiop", "asdfghjklñ", "zxcvbnm", "ES", ",", "espacio", ".", "intro", indent=False),
    )
    screenshot(
        "07-german.png",
        "Deutsch",
        "QWERTZ  ·  stras → straße",
        "Die ",
        "stras",
        ["straße"],
        qwerty_rows("qwertzuiopü", "asdfghjklöä", "yxcvbnmß", "DE", ",", "Leer", ".", "Enter", indent=False),
    )
    screenshot(
        "08-russian.png",
        "Русский",
        "ЙЦУКЕН  ·  прив",
        "",
        "прив",
        ["привет"],
        qwerty_rows("йцукенгшщзх", "фывапролджэ", "ячсмитьбю", "РУ", ",", "пробел", ".", "ввод", indent=False),
    )


def main():
    STORE.mkdir(parents=True, exist_ok=True)
    emit_vectors()
    save_launcher_pngs()
    feature_graphic()
    build_screenshots()
    print("done")


if __name__ == "__main__":
    main()
