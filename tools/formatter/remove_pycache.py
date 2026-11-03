import argparse
import shutil

from common import ROOT


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    removed = []
    for path in ROOT.rglob("__pycache__"):
        if path.is_dir():
            if not args.check:
                shutil.rmtree(path)
            removed.append(path)

    for path in removed:
        print(f"[PYCACHE] {path.relative_to(ROOT)}")
    action = "found" if args.check else "removed"
    print(f"__pycache__ {action}: {len(removed)}")
    return 1 if args.check and removed else 0


if __name__ == "__main__":
    raise SystemExit(main())
