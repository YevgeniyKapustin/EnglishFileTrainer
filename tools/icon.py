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
SPLASH_DURATION_MS = 600


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


def splash_group_xml(index: int, wedge: Wedge) -> str:
    pivot_x, pivot_y = FAN_ORIGIN
    return (
        f'            <group android:name="w{index}" android:pivotX="{pivot_x}" android:pivotY="{pivot_y}" '
        f'android:rotation="{wedge.to_degrees}">\n'
        f"                {wedge_xml_path(wedge)}\n"
        f"            </group>"
    )


def splash_target_xml(index: int, wedge: Wedge) -> str:
    return f"""    <target android:name="w{index}">
        <aapt:attr name="android:animation">
            <objectAnimator android:propertyName="rotation" android:valueFrom="{wedge.to_degrees}" android:valueTo="0"
                android:duration="{SPLASH_DURATION_MS}" android:interpolator="@android:interpolator/fast_out_slow_in" />
        </aapt:attr>
    </target>"""


def splash_icon_xml() -> str:
    groups = NL.join(splash_group_xml(index, wedge) for index, wedge in enumerate(WEDGES))
    targets = NL.join(splash_target_xml(index, wedge) for index, wedge in enumerate(WEDGES))
    return f"""<?xml version="1.0" encoding="utf-8"?>
<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt">
    <aapt:attr name="android:drawable">
        <vector android:width="288dp" android:height="288dp" android:viewportWidth="108" android:viewportHeight="108">
            <clip-path android:pathData="{VISIBLE_CIRCLE}" />
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
