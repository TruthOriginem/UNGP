from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

SKIP_DIRS = {".git", "graphics", "sounds", "tools", "__pycache__"}
SKIP_EXTENSIONS = {
    ".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp", ".tga", ".ico", ".psd",
    ".ogg", ".wav", ".mp3", ".flac", ".wma",
    ".jar", ".class", ".zip", ".7z", ".rar",
    # 对齐 .gitattributes 的 binary 列表：Office 文档/快捷方式是二进制容器，
    # 内部 CRLF 属正常，不得按文本处理
    ".docx", ".lnk",
}


def load_gitignore_patterns() -> list[str]:
    gitignore = ROOT / ".gitignore"
    if not gitignore.is_file():
        return []

    patterns = []
    for line in gitignore.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or line.startswith("!"):
            continue
        patterns.append(line)
    return patterns


GITIGNORE_PATTERNS = load_gitignore_patterns()


def is_gitignored(path: Path) -> bool:
    relative = path.relative_to(ROOT).as_posix()
    for pattern in GITIGNORE_PATTERNS:
        normalized = pattern.strip("/")
        if not normalized:
            continue

        if pattern.endswith("/"):
            if relative == normalized or relative.startswith(normalized + "/"):
                return True
            continue

        if "*" in normalized:
            if Path(relative).match(normalized):
                return True
            continue

        if relative == normalized or relative.startswith(normalized + "/"):
            return True
    return False


def should_skip(path: Path) -> bool:
    return any(part in SKIP_DIRS for part in path.parts) or is_gitignored(path)


def is_text_file(path: Path) -> bool:
    return path.suffix.lower() not in SKIP_EXTENSIONS


def to_lf(content: bytes) -> bytes:
    """Normalize any line ending (CRLF, CR, LF) to LF."""
    return content.replace(b"\r\n", b"\n").replace(b"\r", b"\n")


def normalize_eol(content: bytes, eol: bytes) -> bytes:
    """Normalize any line ending to the given eol (b'\\n' or b'\\r\\n')."""
    return to_lf(content).replace(b"\n", eol)


def iter_text_files():
    for path in ROOT.rglob("*"):
        if not path.is_file():
            continue
        if should_skip(path):
            continue
        if is_text_file(path):
            yield path


def iter_java_files():
    for path in ROOT.rglob("*.java"):
        if not should_skip(path):
            yield path
