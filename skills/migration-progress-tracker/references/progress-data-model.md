# Migration Progress Data Model

State file: `migration-progress.json`

## Root fields
- `project`: string
- `updated_at`: ISO8601
- `epics`: array of epic objects
- `module_backlogs`: object keyed by module name, value is array of module backlog items
- `events`: array of implementation events

## Epic object
- `id` (example `EP-001`)
- `title`
- `status` (`todo|in_progress|done|blocked`)
- `legacy_scope`
- `module_refs` (array of module names)
- `acceptance_summary`
- `items_total`
- `items_done`
- `progress_pct`

## Module backlog item object
- `id` (example `MB-ace-login-001`)
- `epic_id`
- `title`
- `status` (`todo|in_progress|done|blocked`)
- `legacy_refs` (spec/rule references from legacy extraction)
- `acceptance_refs` (acceptance criteria IDs)
- `test_refs` (test case IDs)
- `pr_url`
- `notes`
- `last_update` (ISO8601)

## Event object
- `timestamp` (ISO8601)
- `module`
- `item_id`
- `summary`
- `pr_url`
- `files` (array of file paths)

