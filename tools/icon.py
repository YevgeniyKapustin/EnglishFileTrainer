import math
from pathlib import Path
from typing import NamedTuple

import pymupdf

ROOT = Path(__file__).resolve().parent.parent
DRAWABLE = ROOT / "app/src/main/res/drawable"
PREVIEW = ROOT / "tools/out/icon.png"
NL = "\n"
FAN_ORIGIN = (14.0, 100.0)
VISIBLE_CIRCLE = "M54,18 A36,36 0 1 1 54,90 A36,36 0 1 1 54,18 Z"
SPLASH_DURATION_MS = 520
SPLASH_STAGGER_MS = 20
SPLASH_CENTER = (54.0, 54.0)
SPLASH_RADIUS = 36.0
SPLASH_OVERLAP_DEGREES = 0.6
SPLASH_SWEEPS = (32, 46, 38, 54, 34, 46, 32, 44, 34)
SPLASH_COLORS = (
    "#73B237",
    "#019AA9",
    "#00ABC4",
    "#008297",
    "#027A93",
    "#017F7F",
    "#008196",
    "#01615E",
    "#00ABC4",
)


class Wedge(NamedTuple):
    from_degrees: float
    to_degrees: float
    radius: float
    color: str
    alpha: float


WEDGES = [
    Wedge(96, 76, 72, "#0077C8", 1),
    Wedge(80, 60, 66, "#A8DCF5", 1),
    Wedge(66, 48, 76, "#00A6E2", 1),
    Wedge(54, 32, 82, "#0A7DC6", 1),
    Wedge(35, 18, 76, "#005C8F", 1),
    Wedge(22, 0, 84, "#0A6B86", 1),
    Wedge(74, 56, 52, "#003D66", 0.35),
    Wedge(28, 8, 60, "#002B4D", 0.35),
]


def wedge_path(wedge: Wedge) -> str:
    origin_x, origin_y = FAN_ORIGIN

    def arc_point(degrees: float) -> tuple[float, float]:
        radians = math.radians(degrees)
        return origin_x + wedge.radius * math.cos(radians), origin_y - wedge.radius * math.sin(radians)

    (x1, y1), (x2, y2) = arc_point(wedge.from_degrees), arc_point(wedge.to_degrees)
    radius = wedge.radius
    return f"M{origin_x:.1f},{origin_y:.1f} L{x1:.1f},{y1:.1f} A{radius:.1f},{radius:.1f} 0 0 1 {x2:.1f},{y2:.1f} Z"


def alpha_attribute(alpha: float) -> str:
    return f' android:fillAlpha="{alpha}"' if alpha != 1 else ""


def wedge_xml_path(wedge: Wedge) -> str:
    return (
        f'<path android:fillColor="{wedge.color}"{alpha_attribute(wedge.alpha)} '
        f'android:pathData="{wedge_path(wedge)}" />'
    )


def write_drawable(name: str, text: str) -> None:
    (DRAWABLE / name).write_text(text, encoding="utf-8", newline=NL)


def launcher_foreground_xml() -> str:
    return launcher_xml(f'    <path android:fillColor="#FFFFFF" android:pathData="{VISIBLE_CIRCLE}" />{NL}')


def launcher_xml(backdrop: str) -> str:
    icon_paths = NL.join(f"        {wedge_xml_path(wedge)}" for wedge in WEDGES)
    return f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
{backdrop}    <group>
        <clip-path android:pathData="{VISIBLE_CIRCLE}" />
{icon_paths}
    </group>
</vector>
"""


def splash_sector_path(index: int) -> str:
    center_x, center_y = SPLASH_CENTER
    start_degrees = -90 + sum(SPLASH_SWEEPS[:index])
    end_degrees = start_degrees + SPLASH_SWEEPS[index] + SPLASH_OVERLAP_DEGREES

    def point(degrees: float) -> tuple[float, float]:
        radians = math.radians(degrees)
        return center_x + SPLASH_RADIUS * math.cos(radians), center_y + SPLASH_RADIUS * math.sin(radians)

    start_x, start_y = point(start_degrees)
    end_x, end_y = point(end_degrees)
    return (
        f"M{center_x:.1f},{center_y:.1f} L{start_x:.1f},{start_y:.1f} "
        f"A{SPLASH_RADIUS:.1f},{SPLASH_RADIUS:.1f} 0 0 1 {end_x:.1f},{end_y:.1f} Z"
    )


def splash_folded_path() -> str:
    center_x, center_y = SPLASH_CENTER
    left_degrees = math.radians(-91)
    right_degrees = math.radians(-89)
    left_x = center_x + SPLASH_RADIUS * math.cos(left_degrees)
    left_y = center_y + SPLASH_RADIUS * math.sin(left_degrees)
    right_x = center_x + SPLASH_RADIUS * math.cos(right_degrees)
    right_y = center_y + SPLASH_RADIUS * math.sin(right_degrees)
    return (
        f"M{center_x:.1f},{center_y:.1f} L{left_x:.1f},{left_y:.1f} "
        f"A{SPLASH_RADIUS:.1f},{SPLASH_RADIUS:.1f} 0 0 1 {right_x:.1f},{right_y:.1f} Z"
    )


def splash_group_xml(index: int, color: str) -> str:
    return (
        f'            <path android:name="s{index}" android:fillColor="{color}"'
        f' android:pathData="{splash_folded_path()}" />'
    )


def splash_target_xml(index: int) -> str:
    offset = index * SPLASH_STAGGER_MS
    return f"""    <target android:name="s{index}">
        <aapt:attr name="android:animation">
            <objectAnimator android:propertyName="pathData" android:valueFrom="{splash_folded_path()}"
                android:valueTo="{splash_sector_path(index)}" android:valueType="pathType"
                android:duration="{SPLASH_DURATION_MS}" android:startOffset="{offset}"
                android:interpolator="@android:interpolator/fast_out_slow_in" />
        </aapt:attr>
    </target>"""


def splash_icon_xml() -> str:
    groups = NL.join(splash_group_xml(index, color) for index, color in enumerate(SPLASH_COLORS))
    targets = NL.join(splash_target_xml(index) for index in range(len(SPLASH_COLORS)))
    return f"""<?xml version="1.0" encoding="utf-8"?>
<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt">
    <aapt:attr name="android:drawable">
        <vector android:width="288dp" android:height="288dp" android:viewportWidth="108" android:viewportHeight="108">
{groups}
        </vector>
    </aapt:attr>
{targets}
</animated-vector>
"""


def preview_svg() -> str:
    wedge_paths = [
        f'<path d="{wedge_path(wedge)}" fill="{wedge.color}" fill-opacity="{wedge.alpha}"/>' for wedge in WEDGES
    ]
    return "".join(
        [
            '<svg xmlns="http://www.w3.org/2000/svg" width="432" height="432" viewBox="0 0 108 108">',
            f'<defs><clipPath id="m"><path d="{VISIBLE_CIRCLE}"/></clipPath></defs>',
            '<rect width="108" height="108" fill="#DDDDDD"/>',
            f'<path d="{VISIBLE_CIRCLE}" fill="#FFFFFF"/>',
            '<g clip-path="url(#m)">',
            *wedge_paths,
            '</g><circle cx="54" cy="54" r="70" fill="none" stroke="#DDDDDD" stroke-width="68"/></svg>',
        ]
    )


def main() -> None:
    write_drawable("ic_launcher_foreground.xml", launcher_foreground_xml())
    write_drawable("ic_launcher_monochrome.xml", launcher_xml(""))
    write_drawable("splash_icon.xml", splash_icon_xml())
    PREVIEW.parent.mkdir(exist_ok=True)
    pymupdf.open(stream=preview_svg().encode(), filetype="svg")[0].get_pixmap().save(PREVIEW)


if __name__ == "__main__":
    main()
