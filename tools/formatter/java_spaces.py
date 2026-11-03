import argparse

from common import ROOT, iter_java_files


def leading_tabs_to_spaces(line: str) -> str:
    tabs = len(line) - len(line.lstrip("\t"))
    return "    " * tabs + line[tabs:]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    changed = []
    for path in iter_java_files():
        content = path.read_bytes()
        normalized = content.decode("utf-8").replace("\r\n", "\n").replace("\r", "\n")
        converted = "\n".join(leading_tabs_to_spaces(line) for line in normalized.split("\n"))
        converted = converted.encode("utf-8")

        if converted != content:
            if not args.check:
                path.write_bytes(converted)
            changed.append(path)

    for path in changed:
        print(f"[SPACES] {path.relative_to(ROOT)}")
    action = "needed" if args.check else "converted"
    print(f"Java indentation {action}: {len(changed)}")
    return 1 if args.check and changed else 0


if __name__ == "__main__":
    raise SystemExit(main())
