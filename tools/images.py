import argparse
import io
from dataclasses import dataclass
from pathlib import Path

import pymupdf
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SB_BOOK = ROOT / "english_file" / "EF4 Pre-Int SB.pdf"
WB_BOOK = ROOT / "english_file" / "EF4 Pre-Int WB.pdf"
IMAGE_DIR = ROOT / "app/src/main/assets/image"
SCAN_DPI = 96
WB_RENDER_DPI = 200
POINTS_PER_INCH = 72
INSET = 3
MAX_WIDTH = 480
WEBP_QUALITY = 80

type Box = tuple[int, int, int, int]

VB1A_COLUMNS = ((98, 264), (277, 445), (459, 628))
VB1A_ROWS = ((333, 474), (487, 629), (641, 881))

CROPS: dict[str, tuple[int, Box]] = {
    "1c_sb_painting": (11, (793, 1045, 1536, 1908)),
    "1c_sb_photo": (12, (48, 1045, 744, 1908)),
    "1c_vb1a_1": (152, (610, 196, 765, 352)),
    "1c_vb1a_2": (152, (778, 20, 933, 352)),
    "1c_vb1a_3": (152, (946, 196, 1101, 352)),
    "1c_vb1a_4": (152, (1115, 196, 1270, 352)),
    "1c_vb1a_5": (152, (1283, 196, 1438, 352)),
    "1c_vb1a_6": (152, (610, 364, 765, 520)),
    "1c_vb1a_7": (152, (778, 364, 933, 520)),
    "1c_vb1a_8": (152, (946, 364, 1101, 520)),
    "1c_vb1a_9": (152, (1115, 364, 1270, 520)),
    "1c_vb1a_10": (152, (1283, 364, 1438, 520)),
    "1c_vb1a_11": (152, (610, 532, 765, 688)),
    "1c_vb1a_12": (152, (778, 532, 933, 688)),
    "1c_vb1a_13": (152, (946, 532, 1101, 688)),
    "1c_vb1a_14": (152, (1115, 532, 1270, 688)),
    "1c_vb1a_15": (152, (1283, 532, 1438, 688)),
    "1c_vb1a_16": (152, (610, 700, 765, 856)),
    "1c_vb1a_17": (152, (778, 700, 933, 856)),
    "1c_vb1a_18": (152, (946, 700, 1101, 856)),
    "1c_vb1a_19": (152, (1115, 700, 1270, 856)),
    "1c_vb1a_20": (152, (1283, 700, 1438, 856)),
    "1c_vb1a_21": (152, (610, 890, 765, 1046)),
    "1c_vb1a_22": (152, (778, 890, 933, 1046)),
    "1c_vb1a_23": (152, (946, 890, 1101, 1046)),
    "1c_vb1a_24": (152, (1115, 890, 1270, 1046)),
    "1c_vb1a_25": (152, (1283, 890, 1438, 1046)),
    "1c_vb1a_26": (152, (410, 1080, 605, 1235)),
    "1c_vb1a_27": (152, (610, 1080, 765, 1235)),
    "1c_vb1a_28": (152, (778, 1080, 933, 1235)),
    "1c_vb1a_29": (152, (946, 1080, 1101, 1235)),
    "1c_vb1a_30": (152, (1115, 1080, 1270, 1235)),
    "1c_vb1a_31": (152, (1283, 1080, 1438, 1235)),
    "1c_vb1a_32": (152, (610, 1268, 765, 1424)),
    "1c_vb1a_33": (152, (778, 1268, 933, 1424)),
    "1c_vb1a_34": (152, (946, 1268, 1101, 1424)),
    "1c_vb1a_35": (152, (1115, 1240, 1390, 1470)),
    "2b_sb1a_1": (17, (843, 267, 1497, 1244)),
    "2b_sb4d_a": (18, (329, 1220, 680, 1571)),
    "2b_sb4d_b": (18, (697, 1220, 1048, 1571)),
    "2b_sb4d_c": (18, (1065, 1220, 1416, 1571)),
    "2b_sb4d_d": (18, (340, 1589, 680, 1936)),
    "2b_sb4d_e": (18, (697, 1589, 1048, 1936)),
    "2b_sb4d_f": (18, (1065, 1589, 1416, 1936)),
    "2a_vb1a_2": (153, (369, 301, 628, 505)),
    "2a_vb1a_3": (153, (98, 519, 357, 723)),
    "2a_vb1a_4": (153, (369, 519, 628, 723)),
    "2a_vb1a_5": (153, (98, 736, 357, 941)),
    "2a_vb1a_6": (153, (369, 736, 628, 941)),
    "2a_vb1a_7": (153, (98, 953, 357, 1158)),
    "2a_vb1a_8": (153, (369, 953, 628, 1158)),
    "2a_vb1a_9": (153, (98, 1171, 357, 1376)),
    "2a_vb1a_10": (153, (369, 1171, 628, 1376)),
    **{
        f"1b_vb1a_{row * 3 + column + 1}": (151, (x0, y0, x1, y1))
        for row, (y0, y1) in enumerate(VB1A_ROWS)
        for column, (x0, x1) in enumerate(VB1A_COLUMNS)
        if row or column
    },
    "1a_pe1a_a": (13, (0, 252, 309, 716)),
    "1a_pe1a_b": (13, (322, 252, 744, 716)),
    "1a_pe1a_c": (13, (757, 252, 1438, 716)),
    "1a_pe1a_d": (13, (0, 729, 744, 1235)),
    "1a_pe1a_e": (13, (0, 1249, 393, 1838)),
    "1a_pe1a_f": (13, (407, 1249, 744, 1838)),
}


WB_CROPS: dict[str, tuple[int, Box]] = {
    "1c_wb1c_bedroom": (7, (403, 138, 764, 379)),
    "1c_wb3c_park": (8, (378, 80, 784, 625)),
    "2a_wb3a_2": (12, (205, 97, 340, 192)),
    "2a_wb3a_3": (12, (341, 97, 476, 192)),
    "2a_wb3a_4": (12, (477, 97, 613, 192)),
    "2a_wb3a_5": (12, (614, 97, 744, 192)),
    "2a_wb3a_6": (12, (69, 231, 205, 325)),
    "2a_wb3a_7": (12, (205, 231, 341, 325)),
    "2a_wb3a_8": (12, (342, 231, 476, 325)),
    "2a_wb3a_9": (12, (478, 231, 612, 325)),
    "2a_wb3a_10": (12, (614, 231, 744, 325)),
    "2b_wb2c_2": (14, (303, 107, 518, 349)),
    "2b_wb2c_3": (14, (527, 107, 755, 349)),
    "2b_wb2c_4": (14, (72, 395, 296, 637)),
    "2b_wb2c_5": (14, (300, 395, 519, 637)),
}


@dataclass(frozen=True)
class Book:
    path: Path
    render_dpi: int
    crops: dict[str, tuple[int, Box]]


BOOKS = (Book(SB_BOOK, SCAN_DPI, CROPS), Book(WB_BOOK, WB_RENDER_DPI, WB_CROPS))


def page_rect(box: Box) -> pymupdf.Rect:
    scale = POINTS_PER_INCH / SCAN_DPI
    x0, y0, x1, y1 = box
    return pymupdf.Rect((x0 + INSET) * scale, (y0 + INSET) * scale, (x1 - INSET) * scale, (y1 - INSET) * scale)


def crop(document: pymupdf.Document, page_number: int, box: Box, dpi: int) -> Image.Image:
    pixmap = document[page_number - 1].get_pixmap(dpi=dpi, clip=page_rect(box))
    image = Image.open(io.BytesIO(pixmap.tobytes("png"))).convert("RGB")
    if image.width > MAX_WIDTH:
        image = image.resize((MAX_WIDTH, round(image.height * MAX_WIDTH / image.width)), Image.Resampling.LANCZOS)
    return image


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("names", nargs="*")
    args = parser.parse_args()
    known = {name for book in BOOKS for name in book.crops}
    unknown = [name for name in args.names if name not in known]
    if unknown:
        parser.error(f"нет в CROPS/WB_CROPS: {', '.join(unknown)}")

    IMAGE_DIR.mkdir(parents=True, exist_ok=True)
    total = 0
    for book in BOOKS:
        names = [name for name in args.names or book.crops if name in book.crops]
        if not names:
            continue
        document = pymupdf.open(book.path)
        for name in names:
            page_number, box = book.crops[name]
            path = IMAGE_DIR / f"{name}.webp"
            crop(document, page_number, box, book.render_dpi).save(path, "WEBP", quality=WEBP_QUALITY, method=6)
            total += path.stat().st_size
            print(path.relative_to(ROOT), path.stat().st_size // 1024, "КБ")
    print(f"итого {total / 1024:.0f} КБ")


if __name__ == "__main__":
    main()
