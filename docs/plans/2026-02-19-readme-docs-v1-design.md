# Design: README + Docs Update for v1.0.0

**Date:** 2026-02-19
**Scope:** README.md, docs/rules.md, docs/configuration.md

---

## Goals

1. Clean up README — remove the full rules table, keep it concise
2. Add documentation for 5 undocumented rules in docs/rules.md
3. Update version references to 1.0.0 in all docs

---

## README.md Changes

**Remove:** Full rules tables (43 lines across 6 categories)

**Replace with:** Compact category overview + link to docs/rules.md

```markdown
## Rules

51 rules across 6 categories:

| Category | Rules |
|----------|-------|
| Service Locator | 5 |
| Module DSL | 13 |
| Scope Management | 8 |
| Platform | 8 |
| Architecture | 4 |
| Koin Annotations | 12 |

📖 [Complete Rule Documentation](docs/rules.md)
```

**Also update:** Installation snippet version `0.4.0` → `1.0.0`

---

## docs/rules.md Changes

Add documentation for 5 rules missing from current docs:

### Module DSL

| Rule | What it detects |
|------|----------------|
| `ExcessiveCreatedAtStart` | Too many `createdAtStart = true` → ANR risk. Configurable threshold (default: 10) |
| `ModuleAsTopLevelVal` | `val module = module {}` at top level → factory preallocation. Use `fun` instead |

### Scope Management

| Rule | What it detects |
|------|----------------|
| `ScopeDeclareWithActivityOrFragment` | `scope.declare(activity/fragment)` → memory leak (not cleared on scope close) |

### Architecture

| Rule | What it detects |
|------|----------------|
| `GetConcreteTypeInsteadOfInterface` | `get<ConcreteImpl>()` when only interface is registered → `verify()` false positive |

### Platform

| Rule | What it detects |
|------|----------------|
| `StartKoinInActivity` | `startKoin {}` inside Activity/Fragment → `KoinAppAlreadyStartedException` on rotation |

---

## docs/configuration.md Changes

- Update version `0.3.0` → `1.0.0`
- Add `ExcessiveCreatedAtStart` configuration section (has `maxCreatedAtStart` param)

---

## Decisions

- **Rules table in README:** Removed. Too verbose. Link to docs/rules.md instead.
- **Version:** 1.0.0 everywhere (preparing for 1.0.0 release)
- **Approach:** Full (README + docs), no rules.md redesign
