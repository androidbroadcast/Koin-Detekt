# Field Test Script

Tests detekt-koin against real open-source Koin 4.x projects.

## Usage

```bash
bash scripts/field-test/run.sh             # full run
bash scripts/field-test/run.sh --skip-clone  # re-run analysis only
bash scripts/field-test/run.sh --project MyBrain  # single project
```

## Output

- `reports/<name>.xml` — detekt findings per project
- `reports/<name>.sarif` — SARIF format for IDE import
- `reports/REPORT.md` — aggregated Markdown summary
