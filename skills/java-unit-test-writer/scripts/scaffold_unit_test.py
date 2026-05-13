#!/usr/bin/env python3
"""Scaffold a JUnit test class from a simple template."""

from __future__ import annotations

import argparse
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate a Java *Test.java scaffold.")
    parser.add_argument("--class-name", required=True, help="Production class name, e.g. ItemService")
    parser.add_argument("--package", required=True, help="Test package, e.g. iped.engine.task")
    parser.add_argument(
        "--output-dir",
        required=True,
        help="Base output directory for test sources, e.g. iped-engine/src/test/java",
    )
    parser.add_argument("--force", action="store_true", help="Overwrite file if it already exists.")
    return parser.parse_args()


def load_template() -> str:
    script_dir = Path(__file__).resolve().parent
    template_path = script_dir.parent / "assets" / "templates" / "unit_test_template.java"
    if not template_path.exists():
        raise FileNotFoundError(f"Template file not found: {template_path}")
    return template_path.read_text(encoding="utf-8")


def render(template: str, package_name: str, class_name: str) -> str:
    test_class_name = f"{class_name}Test"
    return (
        template.replace("{{PACKAGE}}", package_name)
        .replace("{{CLASS_NAME}}", class_name)
        .replace("{{TEST_CLASS_NAME}}", test_class_name)
    )


def destination(output_dir: Path, package_name: str, class_name: str) -> Path:
    package_path = Path(*package_name.split("."))
    return output_dir / package_path / f"{class_name}Test.java"


def main() -> int:
    args = parse_args()
    class_name = args.class_name.strip()
    package_name = args.package.strip()
    output_dir = Path(args.output_dir).resolve()

    content = render(load_template(), package_name, class_name)
    target = destination(output_dir, package_name, class_name)
    target.parent.mkdir(parents=True, exist_ok=True)

    if target.exists() and not args.force:
        print(f"[skip] File already exists: {target}")
        print("Use --force to overwrite.")
        return 0

    target.write_text(content, encoding="utf-8")
    print(f"[ok] Test scaffold created: {target}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

