import argparse

from common import ROOT, iter_java_files, normalize_eol


def normalize_java_eof(content: bytes) -> bytes:
    normalized = content.replace(b"\r\n", b"\n").replace(b"\r", b"\n")
    stripped = normalized.rstrip(b"\n")
    if not stripped:
        return b"\n"
    return normalize_eol(stripped, b"\n") + b"\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    changed = []
    for path in iter_java_files():
        content = path.read_bytes()
        converted = normalize_java_eof(content)
        if converted != content:
            if not args.check:
                path.write_bytes(converted)
            changed.append(path)

    for path in changed:
        print(f"[JAVA EOF] {path.relative_to(ROOT)}")
    action = "needed" if args.check else "normalized"
    print(f"Java EOF {action}: {len(changed)}")
    return 1 if args.check and changed else 0


if __name__ == "__main__":
    raise SystemExit(main())
