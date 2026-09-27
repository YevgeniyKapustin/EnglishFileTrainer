import ast
import io
import sys
import tokenize
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
DOCSTRING_OWNERS = (ast.Module, ast.ClassDef, ast.FunctionDef, ast.AsyncFunctionDef)


def comment_lines(source: str) -> list[int]:
    return [
        token.start[0]
        for token in tokenize.generate_tokens(io.StringIO(source).readline)
        if token.type == tokenize.COMMENT
    ]


def docstring_lines(source: str) -> list[int]:
    lines = []
    for node in ast.walk(ast.parse(source)):
        if not isinstance(node, DOCSTRING_OWNERS) or not node.body:
            continue
        first = node.body[0]
        if isinstance(first, ast.Expr) and isinstance(first.value, ast.Constant) and isinstance(first.value.value, str):
            lines.append(first.lineno)
    return lines


def violations(path: Path) -> list[str]:
    source = path.read_text(encoding="utf-8")
    found = [(line, "comment") for line in comment_lines(source)]
    found += [(line, "docstring") for line in docstring_lines(source)]
    return [f"{path}:{line}: {kind}" for line, kind in sorted(found)]


def main() -> None:
    paths = [Path(argument) for argument in sys.argv[1:]] or sorted(TOOLS.glob("*.py"))
    problems = [problem for path in paths for problem in violations(path)]
    for problem in problems:
        print(problem)
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
