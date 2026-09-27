"""Веер лучей в стиле обложки EF4 — для значка и для системной заставки при запуске.

    uv run --no-project --with pymupdf python tools/icon.py

Пишет drawable/ic_launcher_foreground.xml (значок), drawable/splash_icon.xml (анимированный веер
системной заставки) и превью значка в tools/out/icon.png.
"""
import math
from pathlib import Path
import pymupdf as fitz

ROOT = Path(__file__).resolve().parent.parent
DRAWABLE = ROOT / "app/src/main/res/drawable"
PREVIEW = ROOT / "tools/out/icon.png"
NL = "\n"

ORIGIN = (14.0, 100.0)
# (угол от, угол до, радиус, цвет, прозрачность) — углы от горизонтали вверх, радиус в единицах значка (108)
WEDGES = [
    (96, 76, 72, "#0077C8", 1),
    (80, 60, 66, "#A8DCF5", 1),
    (66, 48, 76, "#00A6E2", 1),
    (54, 32, 82, "#0A7DC6", 1),
    (35, 18, 76, "#005C8F", 1),
    (22, 0, 84, "#0A6B86", 1),
    (74, 56, 52, "#003D66", 0.35),
    (28, 8, 60, "#002B4D", 0.35),
]


def wedge_path(a1, a2, r):
    ox, oy = ORIGIN

    def pt(a):
        return ox + r * math.cos(math.radians(a)), oy - r * math.sin(math.radians(a))

    (x1, y1), (x2, y2) = pt(a1), pt(a2)
    return f"M{ox:.1f},{oy:.1f} L{x1:.1f},{y1:.1f} A{r:.1f},{r:.1f} 0 0 1 {x2:.1f},{y2:.1f} Z"


def alpha_attr(al):
    return f' android:fillAlpha="{al}"' if al != 1 else ""


def write(name, text):
    (DRAWABLE / name).write_text(text, encoding="utf-8", newline=NL)


# Круг видимой зоны (72 из 108). Форму маски значка выбирает лаунчер (у TECNO — скруглённый квадрат),
# поэтому фон значка прозрачный, а белый круг и обрезанный по нему веер рисуются здесь — значок круглый везде.
CIRCLE = "M54,18 A36,36 0 1 1 54,90 A36,36 0 1 1 54,18 Z"

# Значок: 108×108, видна середина; фон прозрачный (ic_launcher_background).
icon_paths = NL.join(
    f'        <path android:fillColor="{c}"{alpha_attr(al)} android:pathData="{wedge_path(a1, a2, r)}" />'
    for a1, a2, r, c, al in WEDGES
)
write("ic_launcher_foreground.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Сгенерировано tools/icon.py: значок, веер как на обложке English File 4th. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFF" android:pathData="{CIRCLE}" />
    <group>
        <clip-path android:pathData="{CIRCLE}" />
{icon_paths}
    </group>
</vector>
""")

# Системная заставка Android 12+: тот же веер, анимированный (AVD) — раскрывается, пока грузится приложение.
# Значок заставки без подложки — 288dp с видимым кругом 192dp, та же пропорция, что 108/72 у значка.
# Лучи стартуют сложенными к горизонтали (повёрнуты по часовой на свой нижний угол) и расходятся вверх.
DURATION = 600
groups, targets = [], []
for i, (a1, a2, r, c, al) in enumerate(WEDGES):
    groups.append(f"""            <group android:name="w{i}" android:pivotX="{ORIGIN[0]}" android:pivotY="{ORIGIN[1]}" android:rotation="{a2}">
                <path android:fillColor="{c}"{alpha_attr(al)} android:pathData="{wedge_path(a1, a2, r)}" />
            </group>""")
    targets.append(f"""    <target android:name="w{i}">
        <aapt:attr name="android:animation">
            <objectAnimator android:propertyName="rotation" android:valueFrom="{a2}" android:valueTo="0"
                android:duration="{DURATION}" android:interpolator="@android:interpolator/fast_out_slow_in" />
        </aapt:attr>
    </target>""")
write("splash_icon.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Сгенерировано tools/icon.py: анимированный значок системной заставки (веер раскрывается). -->
<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt">
    <aapt:attr name="android:drawable">
        <vector android:width="288dp" android:height="288dp" android:viewportWidth="108" android:viewportHeight="108">
            <clip-path android:pathData="{CIRCLE}" />
{NL.join(groups)}
        </vector>
    </aapt:attr>
{NL.join(targets)}
</animated-vector>
""")

# Превью значка в круглой маске.
svg = ['<svg xmlns="http://www.w3.org/2000/svg" width="432" height="432" viewBox="0 0 108 108">',
       f'<defs><clipPath id="m"><path d="{CIRCLE}"/></clipPath></defs>',
       '<rect width="108" height="108" fill="#DDDDDD"/>', f'<path d="{CIRCLE}" fill="#FFFFFF"/>', '<g clip-path="url(#m)">']
svg += [f'<path d="{wedge_path(a1, a2, r)}" fill="{c}" fill-opacity="{al}"/>' for a1, a2, r, c, al in WEDGES]
svg.append('</g><circle cx="54" cy="54" r="70" fill="none" stroke="#DDDDDD" stroke-width="68"/></svg>')  # pymupdf не умеет clipPath
PREVIEW.parent.mkdir(exist_ok=True)
fitz.open(stream="".join(svg).encode(), filetype="svg")[0].get_pixmap().save(PREVIEW)
