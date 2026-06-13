# Versioning and Deprecation Policy

## Versioning scheme

`iped-api` follows [Semantic Versioning 2.0.0](https://semver.org/) independently of the
parent IPED project version:

| Change | Version bump | Example |
|--------|-------------|---------|
| New method/type in a stable package | MINOR | 4.1 → 4.2 |
| Breaking change to a stable interface | MAJOR | 4.x → 5.0 |
| Bug fix with no signature change | PATCH | 4.1.0 → 4.1.1 |

The `Automatic-Module-Name` for JPMS consumers is **`iped.api`**.

## Stability tiers

| Tier | Annotation | Policy |
|------|-----------|--------|
| **Stable** | (none) | Deprecated in 4.x; removed no earlier than 5.0 |
| **Experimental** | `@Internal(reason="…")` | May change in any minor release; not covered by this policy |

All public types in `iped-api` are considered **Stable** unless annotated `@Internal`.

## Deprecation process

1. Annotate the target with `@Deprecated` and document the replacement in `@deprecated` Javadoc.
2. Add a note in `MIGRATION_GUIDE.md` under the current release heading.
3. Keep the deprecated element for **at least one minor release** (≥ 3 months) before removal.
4. The element may only be removed in a **MAJOR** version bump.

## Breaking changes

A breaking change is any modification that can cause a compile or runtime error in an
existing plugin without that plugin changing its own code:

- Removing or renaming a type, method, or field in a stable package
- Changing a method signature (adding a required parameter, changing return type)
- Narrowing visibility (public → protected/package-private/private)
- Adding a non-default method to a stable interface (use default methods or a new interface)

Non-breaking additions (adding a default method, a new type, a new constant) do NOT require
a MAJOR bump but must be documented in the release notes.

## Package stability

| Package | Tier |
|---------|------|
| `iped.data.*` | Stable |
| `iped.datasource.*` | Stable |
| `iped.datasource.spi` | Stable |
| `iped.search.*` | Stable |
| `iped.task.*` | Stable |
| `iped.configuration.*` | Stable |
| `iped.pipeline.*` | Stable |
| `iped.io.*` | Stable |
| `iped.properties.*` | Stable |
| `iped.localization.*` | Stable |
| `iped.scripting.*` | Stable (5.0 surface) |
| `iped.exception.*` | Stable |
| `iped.annotation.*` | Stable |
