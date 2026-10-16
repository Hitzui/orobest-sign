"""Genera recursos de icono desde los SVG de assets/branding.

Requisito de herramienta: Pillow==11.3.0 (no es una dependencia de la app).
Ejecutar desde cualquier directorio: python tools/generate_app_icons.py
El renderizador admite los comandos absolutos M/L/H/V/Q/C/Z usados por estos SVG.
"""

from io import BytesIO
from pathlib import Path
import re
import struct
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
BRANDING = ROOT / "assets/branding"
ANDROID = ROOT / "androidApp/src/main/res"
DESKTOP = ROOT / "desktopApp/src/main/resources"
SOURCE = ET.parse(BRANDING / "orobest-sign.svg").getroot()
MONOCHROME = ET.parse(BRANDING / "orobest-sign-monochrome.svg").getroot()
BLUE = SOURCE.find("{*}rect").attrib["fill"]


def paths(data):
    """Devuelve subtrazos muestreados; mantiene curvas a resolución de exportación."""
    tokens = re.findall(r"[MLHVQCZ]|-?\d+(?:\.\d+)?", data)
    i, x, y = 0, 0.0, 0.0
    points = []
    while i < len(tokens):
        command = tokens[i]
        i += 1
        if command == "Z":
            points.append(points[0])
            yield points
            x, y = points[0]
            points = []
            continue
        count = {"M": 2, "L": 2, "H": 1, "V": 1, "Q": 4, "C": 6}[command]
        values = list(map(float, tokens[i:i + count]))
        i += count
        if command == "M":
            if points:
                yield points
            x, y = values
            points = [(x, y)]
        elif command in ("L", "H", "V"):
            x, y = (values if command == "L" else (values[0], y) if command == "H" else (x, values[0]))
            points.append((x, y))
        else:
            start_x, start_y = x, y
            for step in range(1, 49):
                t = step / 48
                u = 1 - t
                if command == "Q":
                    cx, cy, end_x, end_y = values
                    point = (u*u*start_x + 2*u*t*cx + t*t*end_x,
                             u*u*start_y + 2*u*t*cy + t*t*end_y)
                else:
                    ax, ay, bx, by, end_x, end_y = values
                    point = (u**3*start_x + 3*u*u*t*ax + 3*u*t*t*bx + t**3*end_x,
                             u**3*start_y + 3*u*u*t*ay + 3*u*t*t*by + t**3*end_y)
                points.append(point)
            x, y = end_x, end_y
    if points:
        yield points


def render(size, shape="square"):
    # Supermuestreo para trazos limpios en tamaños pequeños.
    high = max(1024, size * 2)
    factor = high / 128
    image = Image.new("RGBA", (high, high))
    draw = ImageDraw.Draw(image)
    if shape == "round":
        draw.ellipse((0, 0, high - 1, high - 1), fill=BLUE)
    elif shape == "rounded":
        draw.rounded_rectangle((0, 0, high - 1, high - 1), radius=high * 0.23, fill=BLUE)
    else:
        draw.rectangle((0, 0, high, high), fill=BLUE)
    for node in SOURCE.findall("{*}path"):
        attributes = node.attrib
        for path in paths(attributes["d"]):
            points = [(x * factor, y * factor) for x, y in path]
            if attributes.get("fill", "none") != "none":
                draw.polygon(points, fill=attributes["fill"])
            if "stroke" in attributes:
                width = max(1, round(float(attributes.get("stroke-width", 1)) * factor))
                draw.line(points, fill=attributes["stroke"], width=width, joint="curve")
                if attributes.get("stroke-linecap") == "round":
                    radius = width / 2
                    for x, y in (points[0], points[-1]):
                        draw.ellipse((x-radius, y-radius, x+radius, y+radius), fill=attributes["stroke"])
    return image.resize((size, size), Image.Resampling.LANCZOS)


def png_bytes(image):
    output = BytesIO()
    image.save(output, format="PNG", optimize=True)
    return output.getvalue()


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    print(path.relative_to(ROOT))


def android_vector(source):
    lines = ['<?xml version="1.0" encoding="utf-8"?>',
             '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
             '    android:width="108dp" android:height="108dp"',
             '    android:viewportWidth="128" android:viewportHeight="128">',
             '    <group android:pivotX="64" android:pivotY="64" android:scaleX="0.70" android:scaleY="0.70">']
    for node in source.findall("{*}path"):
        a = node.attrib
        attributes = {"pathData": a["d"], "fillColor": a.get("fill", "none").replace("none", "#00000000")}
        for svg, android in [("stroke", "strokeColor"), ("stroke-width", "strokeWidth"),
                             ("stroke-linecap", "strokeLineCap"), ("stroke-linejoin", "strokeLineJoin")]:
            if svg in a:
                attributes[android] = a[svg]
        lines.append("        <path " + " ".join(f'android:{key}="{value}"' for key, value in attributes.items()) + "/>")
    lines += ["    </group>", "</vector>", ""]
    return "\n".join(lines).encode()


def main():
    for density, size in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
        write(ANDROID / f"mipmap-{density}/ic_launcher.png", png_bytes(render(size, "rounded")))
        write(ANDROID / f"mipmap-{density}/ic_launcher_round.png", png_bytes(render(size, "round")))
    write(ANDROID / "drawable-v24/ic_launcher_foreground.xml", android_vector(SOURCE))
    write(ANDROID / "drawable/ic_launcher_monochrome.xml", android_vector(MONOCHROME))
    background = f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="{BLUE}" android:pathData="M0,0h108v108h-108z"/>
</vector>
'''
    write(ANDROID / "drawable/ic_launcher_background.xml", background.encode())
    write(ROOT / "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png", png_bytes(render(1024).convert("RGB")))
    write(DESKTOP / "orobest-sign.png", png_bytes(render(512, "rounded")))

    sizes = [16, 24, 32, 48, 64, 128, 256]
    payloads = [png_bytes(render(size, "rounded")) for size in sizes]
    offset = 6 + 16 * len(sizes)
    entries = bytearray(struct.pack("<HHH", 0, 1, len(sizes)))
    for size, payload in zip(sizes, payloads):
        entries += struct.pack("<BBBBHHII", size % 256, size % 256, 0, 0, 1, 32, len(payload), offset)
        offset += len(payload)
    write(DESKTOP / "orobest-sign.ico", entries + b"".join(payloads))

    chunks = []
    for kind, size in [(b"icp4", 16), (b"icp5", 32), (b"icp6", 64), (b"ic07", 128), (b"ic08", 256), (b"ic09", 512), (b"ic10", 1024)]:
        payload = png_bytes(render(size, "rounded"))
        chunks.append(kind + struct.pack(">I", len(payload) + 8) + payload)
    content = b"".join(chunks)
    write(DESKTOP / "orobest-sign.icns", b"icns" + struct.pack(">I", len(content) + 8) + content)
    write(ROOT / "docs/assets/icono-orobest-sign.png", png_bytes(render(256, "rounded")))


if __name__ == "__main__":
    main()
