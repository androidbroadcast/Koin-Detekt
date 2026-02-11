# Claude Code Internal Directory

This directory contains internal files created during development with Claude Code and other AI agents. **All files in this directory are gitignored and should not be committed.**

## Directory Structure

### `.claude/plans/`
Implementation plans, design documents, and architecture decisions created during feature development. These are useful for understanding the development process but are temporary artifacts.

**Examples:**
- `2026-02-11-github-release-automation-design.md`
- `feature-name-implementation-plan.md`

### `.claude/research/`
Research notes, investigation results, and exploration findings. Used when investigating bugs, exploring new libraries, or understanding existing code.

**Examples:**
- `bug-investigation-notes.md`
- `library-comparison.md`
- `performance-analysis.md`

### `.claude/tasks/`
Task breakdowns and todo lists for complex features. Temporary tracking that doesn't belong in project management tools.

**Examples:**
- `feature-tasks.md`
- `refactoring-checklist.md`

### `.claude/scratch/`
Temporary code snippets, experiments, and one-off scripts used during development.

**Examples:**
- `test-script.sh`
- `data-exploration.sql`
- `quick-prototype.kt`

## Guidelines

### ✅ What Goes Here
- Implementation plans and design docs
- Research and investigation notes
- Temporary task lists and checklists
- Code experiments and prototypes
- One-time agent task artifacts
- Development process documentation

### ❌ What Doesn't Go Here
- Production code
- User-facing documentation (use `/docs` instead)
- Configuration files needed for builds
- Shared team documentation
- Anything that should be version controlled

## Why These Are Gitignored

1. **Temporary Nature**: These files serve their purpose during development but become outdated quickly
2. **Context-Specific**: Plans and research are most valuable to the person/agent doing the work
3. **Noise Reduction**: Keeping them out of git history reduces commit noise
4. **Local Value**: Useful locally during development, not needed in repository

## Persistent Documentation

If something in `.claude/` becomes valuable for the team:
- Move design decisions to `/docs/architecture/`
- Move user guides to `/docs/`
- Add architectural decision records (ADRs) to `/docs/decisions/`
- Document APIs in code comments or `/docs/api/`
