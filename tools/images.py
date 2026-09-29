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
    "7a_sb1_office": (55, (98, 478, 832, 780)),
    "7a_sb2_simon": (55, (928, 1396, 1441, 1665)),
    "7a_sb2_claire": (55, (918, 1670, 1451, 1921)),
    "7b_sb1_1": (57, (388, 751, 722, 1085)),
    "7b_sb1_2": (57, (746, 751, 1080, 1085)),
    "7b_sb1_3": (57, (1103, 750, 1438, 1085)),
    "7b_sb1_4": (57, (388, 1108, 722, 1443)),
    "7b_sb1_5": (57, (746, 1108, 1080, 1443)),
    "7b_sb1_6": (57, (1103, 1108, 1438, 1443)),
    "7b_sb1_7": (57, (388, 1466, 722, 1801)),
    "7b_sb1_8": (57, (746, 1466, 1080, 1801)),
    "7b_sb1_9": (57, (1103, 1466, 1438, 1801)),
    "7b_sb2_airi": (58, (140, 245, 860, 597)),
    "7c_sb2_bar": (59, (843, 236, 1533, 864)),
    "7c_comm_1": (111, (908, 701, 1168, 828)),
    "7c_comm_2": (111, (1181, 701, 1442, 828)),
    "7c_comm_3": (111, (908, 841, 1168, 969)),
    "7c_comm_4": (111, (1181, 841, 1442, 967)),
    "7c_comm_5": (111, (908, 981, 1168, 1109)),
    "7c_comm_6": (111, (1181, 981, 1442, 1109)),
    "7c_comm_7": (111, (908, 1122, 1168, 1249)),
    "7c_comm_8": (111, (1181, 1122, 1442, 1249)),
    "7c_comm_9": (111, (908, 1262, 1168, 1390)),
    "7c_comm_10": (111, (1181, 1262, 1442, 1389)),
    "7c_comm_11": (111, (908, 1402, 1168, 1530)),
    "7c_comm_12": (111, (1181, 1402, 1442, 1530)),
    "7c_pe1_1": (61, (3, 392, 372, 723)),
    "7c_pe1_2": (61, (392, 392, 860, 723)),
    "7c_pe2_1": (61, (140, 1354, 377, 1513)),
    "7c_pe2_2": (61, (380, 1354, 615, 1513)),
    "7c_pe2_3": (61, (617, 1354, 856, 1513)),
    "7c_pe2_4": (61, (140, 1516, 377, 1676)),
    "7c_pe2_5": (61, (380, 1516, 615, 1676)),
    "7c_pe2_6": (61, (617, 1516, 856, 1676)),
    "7c_pe3_pharmacy": (61, (908, 383, 1438, 698)),
    "7c_pe4_dinner": (62, (139, 738, 860, 1202)),
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
    "7c_wb1a_1": (49, (78, 230, 258, 328)),
    "7c_wb1a_2": (49, (267, 230, 448, 328)),
    "7c_wb1a_3": (49, (78, 335, 255, 433)),
    "7c_wb1a_4": (49, (267, 335, 448, 433)),
    "7c_wb1b_2": (49, (145, 792, 199, 846)),
    "7c_wb1b_3": (49, (208, 792, 261, 846)),
    "7c_wb1b_4": (49, (270, 792, 323, 846)),
    "7c_wb1b_5": (49, (332, 792, 385, 846)),
    "7c_wb1b_6": (49, (395, 792, 448, 846)),
    "7c_wbpe_pharmacy": (51, (90, 533, 398, 684)),
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
    "7c_comm_1": CORNER_BADGE,
    "7c_comm_2": CORNER_BADGE,
    "7c_comm_3": CORNER_BADGE,
    "7c_comm_4": CORNER_BADGE,
    "7c_comm_5": CORNER_BADGE,
    "7c_comm_6": CORNER_BADGE,
    "7c_comm_7": CORNER_BADGE,
    "7c_comm_8": CORNER_BADGE,
    "7c_comm_9": CORNER_BADGE,
    "7c_comm_10": CORNER_BADGE,
    "7c_comm_11": CORNER_BADGE,
    "7c_comm_12": CORNER_BADGE,
    "7c_pe2_1": CORNER_BADGE,
    "7c_pe2_2": CORNER_BADGE,
    "7c_pe2_3": CORNER_BADGE,
    "7c_pe2_4": CORNER_BADGE,
    "7c_pe2_5": CORNER_BADGE,
    "7c_pe2_6": CORNER_BADGE,
    "7c_wb1a_1": (Blot((2, 5, 21, 24), round=False),),
    "7c_wb1a_2": (Blot((-4, -4, 19, 20), round=False),),
    "7c_wb1a_3": (Blot((2, 3, 21, 22), round=False),),
    "7c_wb1a_4": (Blot((-4, -4, 19, 19), round=False),),
    "7c_wb1b_2": (Blot((-6, -8, 13, 11), round=False),),
    "7c_wb1b_3": (Blot((-6, -8, 13, 11), round=False),),
    "7c_wb1b_4": (Blot((-6, -8, 13, 11), round=False),),
    "7c_wb1b_5": (Blot((-6, -8, 13, 11), round=False),),
    "7c_wb1b_6": (Blot((-6, -8, 13, 11), round=False),),
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
