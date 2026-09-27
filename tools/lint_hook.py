import json
import subprocess
import sys
from pathlib import Path

BLOCKING_EXIT = 2


def edited_path() -> Path | None:
    event = json.load(sys.stdin)
    path = event.get("tool_input", {}).get("file_path")
    return Path(path) if isinstance(path, str) else None


def main() -> None:
    path = edited_path()
    if path is None or path.suffix != ".py" or not path.exists() or not path.resolve().is_relative_to(Path.cwd()):
        return
    checks = [
        ["uvx", "ruff", "format", "--quiet", str(path)],
        ["uvx", "ruff", "check", "--quiet", str(path)],
        [sys.executable, "tools/check_no_comments.py", str(path)],
    ]
    for command in checks:
        result = subprocess.run(command, capture_output=True, text=True, encoding="utf-8", check=False)
        if result.returncode != 0:
            sys.stderr.write(result.stdout + result.stderr)
            sys.exit(BLOCKING_EXIT)


if __name__ == "__main__":
    main()
