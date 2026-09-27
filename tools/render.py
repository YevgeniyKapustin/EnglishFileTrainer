"""Рендер страниц PDF учебника в PNG для распознавания (plans/pipeline.md, шаг 1).

    uv run --no-project --with pymupdf python tools/render.py wb 3 4 --dpi 150
    uv run --no-project --with pymupdf python tools/render.py wb 87 --dpi 200   # ключ

Номера — индекс страницы в PDF, с 1. Картинки — в tools/out/ (не в git: сканы книги).
"""
import argparse
from pathlib import Path

import pymupdf as fitz

ROOT = Path(__file__).resolve().parent.parent
BOOKS = {
    "wb": ROOT / "english_file" / "EF4 Pre-Int WB.pdf",
    "sb": ROOT / "english_file" / "EF4 Pre-Int SB.pdf",
}
OUT = ROOT / "tools" / "out"


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("book", choices=BOOKS)
    p.add_argument("pages", type=int, nargs="+")
    p.add_argument("--dpi", type=int, default=150)
    args = p.parse_args()

    OUT.mkdir(parents=True, exist_ok=True)
    doc = fitz.open(BOOKS[args.book])
    for n in args.pages:
        path = OUT / f"{args.book}-{n:03d}.png"
        doc[n - 1].get_pixmap(dpi=args.dpi).save(path)
        print(path)


if __name__ == "__main__":
    main()
