import argparse
from pathlib import Path

import pymupdf

ROOT = Path(__file__).resolve().parent.parent
BOOKS = {
    "wb": ROOT / "english_file" / "EF4 Pre-Int WB.pdf",
    "sb": ROOT / "english_file" / "EF4 Pre-Int SB.pdf",
}
OUT = ROOT / "tools" / "out"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("book", choices=BOOKS)
    parser.add_argument("pages", type=int, nargs="+")
    parser.add_argument("--dpi", type=int, default=150)
    args = parser.parse_args()

    OUT.mkdir(parents=True, exist_ok=True)
    document = pymupdf.open(BOOKS[args.book])
    for page_number in args.pages:
        path = OUT / f"{args.book}-{page_number:03d}.png"
        document[page_number - 1].get_pixmap(dpi=args.dpi).save(path)
        print(path)


if __name__ == "__main__":
    main()
