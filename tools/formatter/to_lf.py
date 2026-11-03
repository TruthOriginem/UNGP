import argparse

from common import ROOT, iter_text_files, to_lf


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    changed = []
    for path in iter_text_files():
        content = path.read_bytes()
        converted = to_lf(content)
        if converted != content:
            if not args.check:
                path.write_bytes(converted)
            changed.append(path)

    for path in changed:
        print(f"[LF] {path.relative_to(ROOT)}")
    action = "needed" if args.check else "converted"
    print(f"LF {action}: {len(changed)}")
    return 1 if args.check and changed else 0


if __name__ == "__main__":
    raise SystemExit(main())
