#!/usr/bin/env python3
"""Track migration progress and render an HTML status report."""

from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from html import escape
from pathlib import Path
from typing import Any


STATUS_VALUES = ("todo", "in_progress", "done", "blocked")


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat()


def default_state(project: str) -> dict[str, Any]:
    return {
        "project": project,
        "updated_at": utc_now(),
        "epics": [],
        "module_backlogs": {},
        "events": [],
    }


def ensure_parent(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def load_state(path: Path) -> dict[str, Any]:
    if not path.exists():
        raise FileNotFoundError(f"State file not found: {path}")
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError("State JSON must be an object.")
    data.setdefault("project", "migration-project")
    data.setdefault("updated_at", utc_now())
    data.setdefault("epics", [])
    data.setdefault("module_backlogs", {})
    data.setdefault("events", [])
    return data


def save_state(path: Path, state: dict[str, Any]) -> None:
    state["updated_at"] = utc_now()
    ensure_parent(path)
    path.write_text(json.dumps(state, indent=2, ensure_ascii=False), encoding="utf-8")


def default_report_path(state_path: Path) -> Path:
    return state_path.with_name("migration-progress-report.html")


def find_epic(state: dict[str, Any], epic_id: str) -> dict[str, Any] | None:
    for epic in state["epics"]:
        if epic.get("id") == epic_id:
            return epic
    return None


def upsert_epic(
    state: dict[str, Any],
    epic_id: str,
    title: str | None,
    legacy_scope: str | None,
    modules: list[str],
    status: str | None,
    acceptance_summary: str | None,
) -> dict[str, Any]:
    epic = find_epic(state, epic_id)
    if epic is None:
        epic = {
            "id": epic_id,
            "title": title or f"TBD {epic_id}",
            "status": "todo",
            "legacy_scope": legacy_scope or "",
            "module_refs": [],
            "acceptance_summary": acceptance_summary or "",
            "items_total": 0,
            "items_done": 0,
            "progress_pct": 0.0,
        }
        state["epics"].append(epic)

    if title:
        epic["title"] = title
    if legacy_scope is not None:
        epic["legacy_scope"] = legacy_scope
    if acceptance_summary is not None:
        epic["acceptance_summary"] = acceptance_summary
    if status:
        epic["status"] = status
    if modules:
        merged = list(dict.fromkeys([*epic.get("module_refs", []), *modules]))
        epic["module_refs"] = merged
        for module in modules:
            state["module_backlogs"].setdefault(module, [])
    return epic


def upsert_item(
    state: dict[str, Any],
    module: str,
    item_id: str,
    epic_id: str,
    title: str | None,
    status: str | None,
    legacy_refs: list[str],
    acceptance_refs: list[str],
    test_refs: list[str],
    pr_url: str | None,
    notes: str | None,
) -> dict[str, Any]:
    backlog = state["module_backlogs"].setdefault(module, [])

    epic = find_epic(state, epic_id)
    if epic is None:
        epic = upsert_epic(
            state=state,
            epic_id=epic_id,
            title=f"TBD {epic_id}",
            legacy_scope=None,
            modules=[module],
            status="todo",
            acceptance_summary=None,
        )
    elif module not in epic.get("module_refs", []):
        epic["module_refs"] = [*epic.get("module_refs", []), module]

    item = next((x for x in backlog if x.get("id") == item_id), None)
    if item is None:
        item = {
            "id": item_id,
            "epic_id": epic_id,
            "title": title or f"TBD {item_id}",
            "status": status or "todo",
            "legacy_refs": [],
            "acceptance_refs": [],
            "test_refs": [],
            "pr_url": "",
            "notes": "",
            "last_update": utc_now(),
        }
        backlog.append(item)

    if title:
        item["title"] = title
    if status:
        item["status"] = status
    if legacy_refs:
        item["legacy_refs"] = list(dict.fromkeys([*item.get("legacy_refs", []), *legacy_refs]))
    if acceptance_refs:
        item["acceptance_refs"] = list(
            dict.fromkeys([*item.get("acceptance_refs", []), *acceptance_refs])
        )
    if test_refs:
        item["test_refs"] = list(dict.fromkeys([*item.get("test_refs", []), *test_refs]))
    if pr_url is not None:
        item["pr_url"] = pr_url
    if notes is not None:
        item["notes"] = notes
    item["epic_id"] = epic_id
    item["last_update"] = utc_now()
    return item


def add_event(
    state: dict[str, Any],
    module: str,
    item_id: str,
    summary: str,
    pr_url: str | None,
    files: list[str],
) -> None:
    state["events"].append(
        {
            "timestamp": utc_now(),
            "module": module,
            "item_id": item_id,
            "summary": summary,
            "pr_url": pr_url or "",
            "files": files,
        }
    )


def recompute_epic_progress(state: dict[str, Any]) -> None:
    all_backlogs = state.get("module_backlogs", {})
    for epic in state.get("epics", []):
        epic_id = epic.get("id")
        modules = epic.get("module_refs", [])
        candidate_modules = modules if modules else list(all_backlogs.keys())

        items: list[dict[str, Any]] = []
        for module in candidate_modules:
            for item in all_backlogs.get(module, []):
                if item.get("epic_id") == epic_id:
                    items.append(item)

        total = len(items)
        done = sum(1 for item in items if item.get("status") == "done")
        in_progress = any(item.get("status") == "in_progress" for item in items)
        blocked = any(item.get("status") == "blocked" for item in items)
        pct = round((done / total) * 100, 2) if total else 0.0

        if total and done == total:
            status = "done"
        elif blocked:
            status = "blocked"
        elif in_progress:
            status = "in_progress"
        else:
            status = "todo"

        epic["items_total"] = total
        epic["items_done"] = done
        epic["progress_pct"] = pct
        epic["status"] = status


def summarize_backlog(backlogs: dict[str, list[dict[str, Any]]]) -> dict[str, int]:
    counts = {"todo": 0, "in_progress": 0, "done": 0, "blocked": 0}
    for items in backlogs.values():
        for item in items:
            status = item.get("status", "todo")
            if status not in counts:
                counts[status] = 0
            counts[status] += 1
    counts["total"] = sum(counts.values())
    return counts


def render_html(state: dict[str, Any], output_path: Path) -> None:
    summary = summarize_backlog(state.get("module_backlogs", {}))
    done_pct = round((summary["done"] / summary["total"]) * 100, 2) if summary["total"] else 0.0

    epic_rows = []
    for epic in state.get("epics", []):
        modules = ", ".join(epic.get("module_refs", []))
        epic_rows.append(
            "<tr>"
            f"<td>{escape(str(epic.get('id', '')))}</td>"
            f"<td>{escape(str(epic.get('title', '')))}</td>"
            f"<td>{escape(str(epic.get('status', '')))}</td>"
            f"<td>{escape(str(epic.get('progress_pct', 0.0)))}%</td>"
            f"<td>{escape(str(epic.get('items_done', 0)))}/{escape(str(epic.get('items_total', 0)))}</td>"
            f"<td>{escape(modules)}</td>"
            "</tr>"
        )

    module_sections = []
    for module in sorted(state.get("module_backlogs", {}).keys()):
        rows = []
        for item in state["module_backlogs"][module]:
            rows.append(
                "<tr>"
                f"<td>{escape(str(item.get('id', '')))}</td>"
                f"<td>{escape(str(item.get('epic_id', '')))}</td>"
                f"<td>{escape(str(item.get('title', '')))}</td>"
                f"<td>{escape(str(item.get('status', '')))}</td>"
                f"<td>{escape(', '.join(item.get('legacy_refs', [])))}</td>"
                f"<td>{escape(', '.join(item.get('acceptance_refs', [])))}</td>"
                f"<td>{escape(str(item.get('pr_url', '')))}</td>"
                "</tr>"
            )
        module_sections.append(
            f"<h3>{escape(module)}</h3>"
            "<table><thead><tr><th>Item</th><th>Epic</th><th>Title</th><th>Status</th>"
            "<th>Legacy Refs</th><th>Acceptance Refs</th><th>PR</th></tr></thead>"
            f"<tbody>{''.join(rows)}</tbody></table>"
        )

    event_rows = []
    for event in reversed(state.get("events", [])[-100:]):
        event_rows.append(
            "<tr>"
            f"<td>{escape(str(event.get('timestamp', '')))}</td>"
            f"<td>{escape(str(event.get('module', '')))}</td>"
            f"<td>{escape(str(event.get('item_id', '')))}</td>"
            f"<td>{escape(str(event.get('summary', '')))}</td>"
            f"<td>{escape(str(event.get('pr_url', '')))}</td>"
            f"<td>{escape(', '.join(event.get('files', [])))}</td>"
            "</tr>"
        )

    html = f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Migration Progress Report</title>
  <style>
    body {{ font-family: Segoe UI, Arial, sans-serif; margin: 24px; color: #1f2937; }}
    h1, h2, h3 {{ margin: 0 0 12px 0; }}
    .meta {{ margin-bottom: 20px; color: #4b5563; }}
    .cards {{ display: grid; grid-template-columns: repeat(5, minmax(120px, 1fr)); gap: 12px; margin: 14px 0 22px; }}
    .card {{ border: 1px solid #d1d5db; border-radius: 8px; padding: 10px 12px; background: #f9fafb; }}
    .card .label {{ font-size: 12px; color: #6b7280; }}
    .card .value {{ font-size: 20px; font-weight: 700; margin-top: 4px; }}
    table {{ border-collapse: collapse; width: 100%; margin: 8px 0 20px; }}
    th, td {{ border: 1px solid #e5e7eb; padding: 8px 10px; vertical-align: top; font-size: 13px; }}
    th {{ background: #f3f4f6; text-align: left; }}
    .section {{ margin-top: 24px; }}
  </style>
</head>
<body>
  <h1>Migration Progress Report</h1>
  <div class="meta">
    <div><strong>Project:</strong> {escape(str(state.get("project", "")))}</div>
    <div><strong>Updated at:</strong> {escape(str(state.get("updated_at", "")))}</div>
  </div>

  <div class="cards">
    <div class="card"><div class="label">Total Items</div><div class="value">{summary["total"]}</div></div>
    <div class="card"><div class="label">Done</div><div class="value">{summary["done"]}</div></div>
    <div class="card"><div class="label">In Progress</div><div class="value">{summary["in_progress"]}</div></div>
    <div class="card"><div class="label">Blocked</div><div class="value">{summary["blocked"]}</div></div>
    <div class="card"><div class="label">Completion</div><div class="value">{done_pct}%</div></div>
  </div>

  <div class="section">
    <h2>Global Roadmap (Epics)</h2>
    <table>
      <thead>
        <tr><th>Epic</th><th>Title</th><th>Status</th><th>Progress</th><th>Done/Total</th><th>Modules</th></tr>
      </thead>
      <tbody>{''.join(epic_rows)}</tbody>
    </table>
  </div>

  <div class="section">
    <h2>Module Backlogs</h2>
    {''.join(module_sections)}
  </div>

  <div class="section">
    <h2>Recent Implementation Events</h2>
    <table>
      <thead>
        <tr><th>Timestamp</th><th>Module</th><th>Item</th><th>Summary</th><th>PR</th><th>Files</th></tr>
      </thead>
      <tbody>{''.join(event_rows)}</tbody>
    </table>
  </div>
</body>
</html>
"""
    ensure_parent(output_path)
    output_path.write_text(html, encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Update migration progress tracker.")
    sub = parser.add_subparsers(dest="command", required=True)

    p_init = sub.add_parser("init", help="Create tracker state JSON and HTML report.")
    p_init.add_argument("--file", required=True, help="Path to migration-progress.json")
    p_init.add_argument("--project", default="migration-project", help="Project name")
    p_init.add_argument("--force", action="store_true", help="Overwrite existing file")

    p_epic = sub.add_parser("upsert-epic", help="Create or update an epic.")
    p_epic.add_argument("--file", required=True)
    p_epic.add_argument("--epic-id", required=True)
    p_epic.add_argument("--title")
    p_epic.add_argument("--legacy-scope")
    p_epic.add_argument("--module", action="append", default=[])
    p_epic.add_argument("--status", choices=STATUS_VALUES)
    p_epic.add_argument("--acceptance-summary")

    p_item = sub.add_parser("upsert-item", help="Create or update module backlog item.")
    p_item.add_argument("--file", required=True)
    p_item.add_argument("--module", required=True)
    p_item.add_argument("--item-id", required=True)
    p_item.add_argument("--epic-id", required=True)
    p_item.add_argument("--title")
    p_item.add_argument("--status", choices=STATUS_VALUES)
    p_item.add_argument("--legacy-ref", action="append", default=[])
    p_item.add_argument("--acceptance-ref", action="append", default=[])
    p_item.add_argument("--test-ref", action="append", default=[])
    p_item.add_argument("--pr-url")
    p_item.add_argument("--notes")

    p_event = sub.add_parser("add-event", help="Append an implementation event.")
    p_event.add_argument("--file", required=True)
    p_event.add_argument("--module", required=True)
    p_event.add_argument("--item-id", required=True)
    p_event.add_argument("--summary", required=True)
    p_event.add_argument("--pr-url")
    p_event.add_argument("--changed-file", action="append", default=[])

    p_render = sub.add_parser("render", help="Render HTML report from state file.")
    p_render.add_argument("--file", required=True)
    p_render.add_argument("--output")
    return parser.parse_args()


def run_mutation_with_report(state_path: Path, state: dict[str, Any]) -> None:
    recompute_epic_progress(state)
    save_state(state_path, state)
    report_path = default_report_path(state_path)
    render_html(state, report_path)
    print(f"[ok] Updated: {state_path}")
    print(f"[ok] Report:  {report_path}")


def main() -> int:
    args = parse_args()

    if args.command == "init":
        state_path = Path(args.file).resolve()
        if state_path.exists() and not args.force:
            raise FileExistsError(f"State file already exists: {state_path}. Use --force to overwrite.")
        state = default_state(args.project)
        save_state(state_path, state)
        report_path = default_report_path(state_path)
        render_html(state, report_path)
        print(f"[ok] Initialized: {state_path}")
        print(f"[ok] Report:      {report_path}")
        return 0

    state_path = Path(args.file).resolve()
    state = load_state(state_path)

    if args.command == "upsert-epic":
        upsert_epic(
            state=state,
            epic_id=args.epic_id,
            title=args.title,
            legacy_scope=args.legacy_scope,
            modules=[m.strip() for m in args.module if m and m.strip()],
            status=args.status,
            acceptance_summary=args.acceptance_summary,
        )
        run_mutation_with_report(state_path, state)
        return 0

    if args.command == "upsert-item":
        upsert_item(
            state=state,
            module=args.module.strip(),
            item_id=args.item_id.strip(),
            epic_id=args.epic_id.strip(),
            title=args.title,
            status=args.status,
            legacy_refs=[x.strip() for x in args.legacy_ref if x and x.strip()],
            acceptance_refs=[x.strip() for x in args.acceptance_ref if x and x.strip()],
            test_refs=[x.strip() for x in args.test_ref if x and x.strip()],
            pr_url=args.pr_url,
            notes=args.notes,
        )
        run_mutation_with_report(state_path, state)
        return 0

    if args.command == "add-event":
        add_event(
            state=state,
            module=args.module.strip(),
            item_id=args.item_id.strip(),
            summary=args.summary.strip(),
            pr_url=args.pr_url,
            files=[x.strip() for x in args.changed_file if x and x.strip()],
        )
        run_mutation_with_report(state_path, state)
        return 0

    if args.command == "render":
        report_path = Path(args.output).resolve() if args.output else default_report_path(state_path)
        recompute_epic_progress(state)
        render_html(state, report_path)
        print(f"[ok] Report: {report_path}")
        return 0

    raise ValueError(f"Unsupported command: {args.command}")


if __name__ == "__main__":
    raise SystemExit(main())

