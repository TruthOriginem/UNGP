import argparse

from common import ROOT, iter_text_files


UTF8_BOM = b"\xef\xbb\xbf"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    changed = []
    for path in iter_text_files():
        content = path.read_bytes()
        if not content.startswith(UTF8_BOM):
            continue

        if not args.check:
            path.write_bytes(content[len(UTF8_BOM):])
        changed.append(path)

    for path in changed:
        print(f"[UTF8 BOM] {path.relative_to(ROOT)}")
    action = "found" if args.check else "removed"
    print(f"UTF-8 BOM {action}: {len(changed)}")
    return 1 if args.check and changed else 0


if __name__ == "__main__":
    raise SystemExit(main())
