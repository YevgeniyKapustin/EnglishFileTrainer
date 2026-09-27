import argparse
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app/src/main/assets"
FOLDERS = ("audio", "image", "video")
DEFAULT_BACKUP = Path.home() / "OneDrive/english-file-trainer-assets"


def differs(source: Path, target: Path) -> bool:
    if not target.exists():
        return True
    return source.stat().st_size != target.stat().st_size or source.stat().st_mtime > target.stat().st_mtime + 1


def mirror(source_root: Path, target_root: Path) -> tuple[int, int]:
    copied = kept = 0
    for folder in FOLDERS:
        for source in sorted((source_root / folder).rglob("*")):
            if not source.is_file():
                continue
            target = target_root / source.relative_to(source_root)
            if differs(source, target):
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, target)
                copied += 1
            else:
                kept += 1
    return copied, kept


def only_in(first: Path, second: Path) -> list[Path]:
    return [
        path.relative_to(first)
        for folder in FOLDERS
        for path in sorted((first / folder).rglob("*"))
        if path.is_file() and not (second / path.relative_to(first)).exists()
    ]


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description="Копия ассетов вне репозитория; ничего не удаляет ни с одной стороны")
    parser.add_argument("mode", choices=["backup", "restore", "status"])
    parser.add_argument("--to", type=Path, default=DEFAULT_BACKUP, help="папка копии")
    args = parser.parse_args()
    backup: Path = args.to
    if args.mode == "status":
        missing_backup = only_in(ASSETS, backup)
        missing_local = only_in(backup, ASSETS) if backup.exists() else []
        print(f"копия: {backup}")
        print(f"нет в копии: {len(missing_backup)}; нет в проекте: {len(missing_local)}")
        for path in missing_local:
            print(f"  нет в проекте: {path}")
        return
    source, target = (ASSETS, backup) if args.mode == "backup" else (backup, ASSETS)
    if not source.exists():
        raise SystemExit(f"нет папки {source}")
    copied, kept = mirror(source, target)
    print(f"{args.mode}: скопировано {copied}, без изменений {kept} → {target}")


if __name__ == "__main__":
    main()
