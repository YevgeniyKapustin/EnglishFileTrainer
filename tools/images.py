import argparse
from dataclasses import dataclass
from pathlib import Path

import pymupdf
from PIL import Image, ImageDraw
from realesrgan_ncnn_py import Realesrgan
from simple_lama_inpainting import SimpleLama

ROOT = Path(__file__).resolve().parent.parent
SB_BOOK = ROOT / "english_file" / "EF4 Pre-Int SB.pdf"
WB_BOOK = ROOT / "english_file" / "EF4 Pre-Int WB.pdf"
IMAGE_DIR = ROOT / "app/src/main/assets/image"
SCAN_DPI = 96
POINTS_PER_INCH = 72
INSET = 3
OUTPUT_WIDTH = 1080
WEBP_QUALITY = 82
UPSCALE_MODEL = 4

type Box = tuple[int, int, int, int]


@dataclass(frozen=True)
class Blot:
    box: Box
    round: bool = True


VB1A_COLUMNS = ((98, 264), (277, 445), (459, 628))
VB1A_ROWS = ((333, 474), (487, 629), (641, 881))

CROPS: dict[str, tuple[int, Box]] = {
    "1c_sb_painting": (11, (793, 1045, 1536, 1908)),
    "1c_sb_photo": (12, (48, 1045, 744, 1908)),
    "1c_sb6a_1": (103, (57, 1045, 648, 1822)),
    "1c_sb6a_2": (103, (678, 1318, 1438, 1822)),
    "2c_sb1_1": (19, (958, 238, 1536, 535)),
    "2c_sb1_2": (19, (858, 535, 1438, 887)),
    "2c_sb1_3": (19, (140, 597, 858, 905)),
    "2c_sb1_4": (19, (0, 990, 545, 1297)),
    "2c_sb1_5": (19, (238, 1298, 678, 1604)),
    "2c_sb1_6": (19, (42, 1606, 620, 1860)),
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
    "1b_sb1a": (9, (678, 408, 1536, 905)),
    "1b_sb3c_maggie": (103, (677, 371, 1438, 576)),
    "1b_wr1_charlie": (114, (946, 318, 1402, 922)),
    "2b_wr2_storks": (115, (133, 308, 1410, 926)),
    "10a_sb1a_2": (79, (0, 238, 512, 465)),
    "10a_sb1a_3": (79, (0, 467, 512, 694)),
    "10a_sb1a_4": (79, (513, 278, 767, 694)),
    "10a_sb1a_5": (79, (769, 238, 1278, 465)),
    "10a_sb1a_6": (79, (769, 467, 1278, 694)),
    "10a_sb1a_7": (79, (1280, 238, 1536, 694)),
    "10a_vb1a_1": (163, (677, 196, 1050, 427)),
    "10a_vb1a_2": (163, (1064, 196, 1437, 427)),
    "10a_vb1a_3": (163, (677, 442, 1050, 672)),
    "10a_vb1a_4": (163, (1064, 442, 1437, 672)),
    "10a_vb1a_5": (163, (677, 687, 1050, 917)),
    "10a_vb1a_6": (163, (1064, 687, 1437, 917)),
    "10a_vb1a_7": (163, (677, 933, 1050, 1163)),
    "10a_vb1a_8": (163, (1064, 933, 1437, 1163)),
    "10a_vb1a_9": (163, (677, 1178, 1050, 1408)),
    "10a_vb1a_10": (163, (1064, 1178, 1437, 1408)),
    "10a_vb1a_11": (163, (677, 1424, 1050, 1654)),
    "10a_vb1a_12": (163, (1064, 1424, 1437, 1654)),
    "10b_vb1a_1": (164, (98, 701, 344, 904)),
    "10b_vb1a_2": (164, (357, 701, 603, 904)),
    "10b_vb1a_3": (164, (617, 701, 862, 904)),
    "10b_vb1a_4": (164, (98, 919, 344, 1121)),
    "10b_vb1a_5": (164, (357, 919, 603, 1121)),
    "10b_vb1a_6": (164, (617, 919, 862, 1121)),
    "10b_vb1a_7": (164, (98, 1136, 344, 1339)),
    "10b_vb1a_8": (164, (357, 1136, 603, 1339)),
    "10b_vb1a_9": (164, (617, 1136, 862, 1339)),
    "10b_vb1a_10": (164, (98, 1354, 344, 1556)),
    "10b_vb1a_11": (164, (357, 1354, 603, 1774)),
    "10b_vb1a_12": (164, (617, 1354, 862, 1556)),
    "10b_vb1a_13": (164, (98, 1571, 344, 1774)),
    "10b_vb1a_14": (164, (617, 1571, 862, 1774)),
    "10b_sb1_ella": (81, (0, 1115, 708, 1730)),
    "10b_sb1_peter": (81, (729, 1115, 1438, 1730)),
    "10c_sb4a_1": (84, (838, 388, 1034, 531)),
    "10c_sb4a_2": (84, (1036, 388, 1236, 531)),
    "10c_sb4a_3": (84, (1238, 388, 1438, 531)),
    "10c_sb4a_4": (84, (838, 533, 1034, 679)),
    "10c_sb4a_5": (84, (1036, 533, 1236, 679)),
    "10c_sb4a_6": (84, (1238, 533, 1438, 679)),
    "10c_sb4a_7": (84, (838, 681, 1034, 825)),
    "10c_sb4a_8": (84, (1036, 681, 1236, 825)),
    "10c_sb4a_9": (84, (1238, 681, 1438, 825)),
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
    "10a_wb1a_2": (67, (219, 267, 346, 371)),
    "10a_wb1a_3": (67, (77, 398, 205, 501)),
    "10a_wb1a_4": (67, (219, 398, 346, 501)),
    "10a_wb1a_5": (67, (77, 528, 205, 632)),
    "10a_wb1a_6": (67, (219, 528, 346, 632)),
    "10a_wb1a_7": (67, (77, 659, 205, 762)),
    "10a_wb1a_8": (67, (219, 659, 346, 762)),
    "10a_wb3b_2": (68, (588, 102, 741, 230)),
    "10a_wb3b_3": (68, (428, 236, 582, 364)),
    "10a_wb3b_4": (68, (588, 236, 741, 364)),
    "10a_wb3b_5": (68, (428, 370, 582, 498)),
    "10a_wb3b_6": (68, (588, 370, 741, 498)),
    "10b_wb1a_2": (69, (207, 197, 330, 300)),
    "10b_wb1a_3": (69, (332, 197, 450, 300)),
    "10b_wb1a_4": (69, (80, 310, 190, 428)),
    "10b_wb1a_5": (69, (207, 310, 330, 428)),
    "10b_wb1a_6": (69, (335, 310, 450, 428)),
    "10c_wb3d_2": (72, (554, 72, 765, 245)),
    "10c_wb3d_3": (72, (617, 270, 780, 433)),
    "10c_wb3d_4": (72, (452, 310, 612, 512)),
    "10c_wb3d_5": (72, (448, 512, 785, 678)),
}

CORNER_BADGE = (Blot((-2, -2, 38, 39)),)
LETTER_BADGE = (Blot((5, 5, 45, 45)), Blot((47, 5, 88, 45), round=False))
NUMBER_BADGE = (Blot((12, 11, 52, 52)),)

ERASE: dict[str, tuple[Blot, ...]] = {
    **{name: CORNER_BADGE for name in CROPS if "_vb1a_" in name},
    "1c_vb1a_2": (Blot((-2, 172, 38, 215)),),
    "1c_vb1a_26": (Blot((29, -2, 70, 39)),),
    "1c_vb1a_35": (Blot((-2, 31, 39, 71)),),
    "1a_pe1a_a": (Blot((215, 11, 254, 50)), Blot((257, 11, 297, 51), round=False)),
    "1a_pe1a_b": (Blot((13, 11, 52, 52)), Blot((55, 11, 94, 52), round=False)),
    "1a_pe1a_c": (Blot((13, 11, 52, 52)), Blot((55, 11, 94, 52), round=False)),
    "1a_pe1a_d": (Blot((96, 12, 135, 50)), Blot((138, 12, 178, 51), round=False)),
    "1a_pe1a_e": (Blot((96, 11, 135, 51)), Blot((138, 11, 178, 51), round=False)),
    "1a_pe1a_f": (Blot((20, 11, 58, 50)), Blot((61, 12, 100, 51), round=False)),
    **{f"2b_sb4d_{letter}": LETTER_BADGE for letter in "abcef"},
    "2b_sb4d_d": (Blot((-2, 5, 34, 44)), Blot((36, 5, 76, 44), round=False)),
    **{f"2c_sb1_{n}": NUMBER_BADGE for n in (1, 2, 3, 5, 6)},
    "2c_sb1_4": (Blot((40, 12, 79, 51)),),
    "10a_sb1a_2": (Blot((62, 192, 98, 228)),),
    "10a_sb1a_3": (Blot((62, 192, 98, 228)),),
    "10a_sb1a_4": (Blot((-2, 370, 50, 420), round=False),),
    "10a_sb1a_5": (Blot((-2, 192, 35, 228)),),
    "10a_sb1a_6": (Blot((-2, 192, 35, 228)),),
    "10a_sb1a_7": (Blot((-1, 421, 36, 457)),),
    **{f"10c_sb4a_{n}": CORNER_BADGE for n in range(1, 10)},
    **{f"10a_wb3b_{n}": (Blot((-2, -2, 27, 27)),) for n in range(2, 7)},
    **{f"10b_wb1a_{n}": (Blot((-2, -2, 20, 20)),) for n in (2, 4, 5, 6)},
    "10b_wb1a_3": (Blot((0, -2, 23, 20)),),
    "10c_wb3d_2": (Blot((26, 155, 48, 175)),),
    "10c_wb3d_3": (Blot((20, 143, 41, 165)),),
    "10c_wb3d_4": (Blot((133, 157, 161, 182), round=False),),
    "10c_wb3d_5": (Blot((12, 146, 34, 168)),),
}


@dataclass(frozen=True)
class Book:
    path: Path
    crops: dict[str, tuple[int, Box]]


BOOKS = (Book(SB_BOOK, CROPS), Book(WB_BOOK, WB_CROPS))


def page_rect(box: Box) -> pymupdf.Rect:
    scale = POINTS_PER_INCH / SCAN_DPI
    x0, y0, x1, y1 = box
    return pymupdf.Rect((x0 + INSET) * scale, (y0 + INSET) * scale, (x1 - INSET) * scale, (y1 - INSET) * scale)


def scan_zoom(page: pymupdf.Page) -> float:
    widest = max(page.get_image_info(), key=lambda info: info["width"])
    x0, _, x1, _ = widest["bbox"]
    span: float = x1 - x0
    return float(widest["width"]) / span


def blot_mask(size: tuple[int, int], blots: tuple[Blot, ...], scale: float) -> Image.Image:
    mask = Image.new("L", size, 0)
    draw = ImageDraw.Draw(mask)
    for blot in blots:
        x0, y0, x1, y1 = ((value - INSET) * scale for value in blot.box)
        if blot.round:
            draw.ellipse((x0, y0, x1, y1), fill=255)
        else:
            draw.rectangle((x0, y0, x1, y1), fill=255)
    return mask


def erase(image: Image.Image, blots: tuple[Blot, ...], scale: float, painter: SimpleLama) -> Image.Image:
    painted: Image.Image = painter(image, blot_mask(image.size, blots, scale))
    return painted.crop((0, 0, image.width, image.height))


def crop(document: pymupdf.Document, page_number: int, box: Box) -> tuple[Image.Image, float]:
    page = document[page_number - 1]
    zoom = scan_zoom(page)
    pixmap = page.get_pixmap(matrix=pymupdf.Matrix(zoom, zoom), clip=page_rect(box))
    image = Image.frombytes("RGB", (pixmap.width, pixmap.height), pixmap.samples)
    return image, zoom * POINTS_PER_INCH / SCAN_DPI


def sharpen(image: Image.Image, upscaler: Realesrgan) -> Image.Image:
    upscaled: Image.Image = upscaler.process_pil(image)
    height = round(upscaled.height * OUTPUT_WIDTH / upscaled.width)
    return upscaled.resize((OUTPUT_WIDTH, height), Image.Resampling.LANCZOS)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("names", nargs="*")
    args = parser.parse_args()
    known = {name for book in BOOKS for name in book.crops}
    unknown = [name for name in args.names if name not in known]
    if unknown:
        parser.error(f"нет в CROPS/WB_CROPS: {', '.join(unknown)}")

    IMAGE_DIR.mkdir(parents=True, exist_ok=True)
    upscaler = Realesrgan(model=UPSCALE_MODEL)
    painter = SimpleLama()
    total = 0
    for book in BOOKS:
        names = [name for name in args.names or book.crops if name in book.crops]
        if not names:
            continue
        document = pymupdf.open(book.path)
        for name in names:
            page_number, box = book.crops[name]
            path = IMAGE_DIR / f"{name}.webp"
            image, scale = crop(document, page_number, box)
            if name in ERASE:
                image = erase(image, ERASE[name], scale, painter)
            sharpen(image, upscaler).save(path, "WEBP", quality=WEBP_QUALITY, method=6)
            total += path.stat().st_size
            print(path.relative_to(ROOT), path.stat().st_size // 1024, "КБ")
    print(f"итого {total / 1024:.0f} КБ")


if __name__ == "__main__":
    main()
