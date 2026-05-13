#!/usr/bin/env python3
"""Initialize a reverse-engineering spec pack from bundled templates."""

from __future__ import annotations

import argparse
from datetime import date
from pathlib import Path


def render_text(text: str, project: str, modules: list[str]) -> str:
    module_list = ", ".join(modules) if modules else "TBD"
    return (
        text.replace("{{PROJECT_NAME}}", project)
        .replace("{{DATE}}", date.today().isoformat())
        .replace("{{MODULE_LIST}}", module_list)
    )


def copy_templates(output_dir: Path, project: str, modules: list[str], force: bool) -> None:
    script_dir = Path(__file__).resolve().parent
    template_dir = script_dir.parent / "assets" / "templates" / "spec-pack"
    if not template_dir.exists():
        raise FileNotFoundError(f"Template directory not found: {template_dir}")

    output_dir.mkdir(parents=True, exist_ok=True)
    for src in sorted(template_dir.rglob("*")):
        if src.is_dir():
            continue
        rel = src.relative_to(template_dir)
        dst = output_dir / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        if dst.exists() and not force:
            continue
        content = src.read_text(encoding="utf-8")
        dst.write_text(render_text(content, project, modules), encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Initialize legacy reverse-spec pack.")
    parser.add_argument("--output", required=True, help="Output folder for generated spec files.")
    parser.add_argument("--project", default="legacy-system", help="Project name token.")
    parser.add_argument(
        "--module",
        action="append",
        default=[],
        help="Scoped module name. Repeat option for multiple modules.",
    )
    parser.add_argument("--force", action="store_true", help="Overwrite existing files.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    output = Path(args.output).resolve()
    modules = [m.strip() for m in args.module if m and m.strip()]
    copy_templates(output, args.project.strip(), modules, args.force)
    print(f"[ok] Spec pack initialized at: {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

