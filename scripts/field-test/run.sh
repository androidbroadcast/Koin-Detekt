#\!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 [--skip-clone] [--project <name>]"
  echo ""
  echo "Options:"
  echo "  --skip-clone     Skip git clone/pull (use existing repos/)"
  echo "  --project <name> Analyze only one project from projects.conf"
  exit 0
}

SKIP_CLONE=false
ONLY_PROJECT=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-clone) SKIP_CLONE=true; shift ;;
    --project) ONLY_PROJECT="$2"; shift 2 ;;
    --help|-h) usage ;;
    *) echo "Unknown option: $1"; usage ;;
  esac
done

# Paths — all relative to this script's directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOS_DIR="$SCRIPT_DIR/repos"
REPORTS_DIR="$SCRIPT_DIR/reports"
CONF="$SCRIPT_DIR/projects.conf"

DETEKT_VERSION="1.23.8"
PLUGIN_VERSION="1.0.0"
DETEKT_JAR="$SCRIPT_DIR/detekt-cli-${DETEKT_VERSION}.jar"
PLUGIN_JAR="$SCRIPT_DIR/detekt-koin4-rules-${PLUGIN_VERSION}.jar"
CONFIG="$SCRIPT_DIR/detekt-koin-all-rules.yml"

# ── Bootstrap ──────────────────────────────────────────────────────────────────

bootstrap() {
  mkdir -p "$REPOS_DIR" "$REPORTS_DIR"

  # detekt CLI
  if [[ \! -f "$DETEKT_JAR" ]]; then
    echo "→ Downloading detekt-cli ${DETEKT_VERSION}..."
    curl -sSL \
      "https://github.com/detekt/detekt/releases/download/v${DETEKT_VERSION}/detekt-cli-${DETEKT_VERSION}-all.jar" \
      -o "$DETEKT_JAR"
  else
    echo "✓ detekt-cli already present"
  fi

  # Plugin JAR — prefer local build, fall back to Maven Central
  if [[ \! -f "$PLUGIN_JAR" ]]; then
    LOCAL_JAR="$(find "$SCRIPT_DIR/../../build/libs" -name "detekt-koin4-rules-${PLUGIN_VERSION}.jar" 2>/dev/null | head -1)"
    if [[ -n "$LOCAL_JAR" ]]; then
      echo "→ Using local build: $LOCAL_JAR"
      cp "$LOCAL_JAR" "$PLUGIN_JAR"
    else
      echo "→ Downloading detekt-koin4-rules ${PLUGIN_VERSION} from Maven Central..."
      curl -sSL \
        "https://repo1.maven.org/maven2/dev/androidbroadcast/rules/koin/detekt-koin4-rules/${PLUGIN_VERSION}/detekt-koin4-rules-${PLUGIN_VERSION}.jar" \
        -o "$PLUGIN_JAR"
    fi
  else
    echo "✓ plugin JAR already present"
  fi

  # Config
  if [[ \! -f "$CONFIG" ]]; then
    cp "$SCRIPT_DIR/../../config/detekt-koin-all-rules.yml" "$CONFIG"
    echo "✓ config copied"
  fi
}

# ── Clone / Update ─────────────────────────────────────────────────────────────

clone_projects() {
  if [[ "$SKIP_CLONE" == true ]]; then
    echo "→ Skipping clone (--skip-clone)"
    return
  fi

  while IFS=$'\t' read -r name repo branch _src_dirs; do
    [[ "$name" =~ ^#.*$ || -z "$name" ]] && continue  # skip comments/blanks

    local dest="$REPOS_DIR/$name"
    if [[ -d "$dest/.git" ]]; then
      echo "→ Updating $name..."
      git -C "$dest" pull --quiet --ff-only
    else
      echo "→ Cloning $name ($repo @ $branch)..."
      git clone --depth 1 --branch "$branch" \
        "https://github.com/$repo.git" "$dest" --quiet
    fi
  done < <(grep -v '^#' "$CONF" | grep -v '^[[:space:]]*$')
}

# ── Analysis ───────────────────────────────────────────────────────────────────

run_detekt() {
  local name="$1"
  local repo_dir="$REPOS_DIR/$name"
  local src_dirs_raw="$2"  # comma-separated relative paths

  # Build --input argument: absolute paths, comma-separated, only existing dirs
  local input_paths=""
  IFS=',' read -ra dirs <<< "$src_dirs_raw"
  for rel_dir in "${dirs[@]}"; do
    local abs_dir="$repo_dir/$rel_dir"
    if [[ -d "$abs_dir" ]]; then
      input_paths="${input_paths:+$input_paths,}$abs_dir"
    else
      echo "  ⚠ $name: src dir not found: $rel_dir (skipping)"
    fi
  done

  if [[ -z "$input_paths" ]]; then
    echo "✗ $name: no valid source directories found, skipping"
    return
  fi

  echo "→ Analyzing $name..."

  java -jar "$DETEKT_JAR" \
    --plugins "$PLUGIN_JAR" \
    --config "$CONFIG" \
    --input "$input_paths" \
    --excludes "**/test/**,**/androidTest/**,**/commonTest/**,**/iosTest/**,**/*.kts" \
    --report "xml:$REPORTS_DIR/${name}.xml" \
    --report "sarif:$REPORTS_DIR/${name}.sarif" \
    --build-upon-default-config \
    --no-default-rulesets \
    2>"$REPORTS_DIR/${name}.log" \
    && echo "✓ $name: done" \
    || echo "✗ $name: detekt exited with errors (check ${name}.log)"
}

analyze_projects() {
  local pids=()

  while IFS=$'\t' read -r name _repo _branch src_dirs; do
    [[ "$name" =~ ^#.*$ || -z "$name" ]] && continue
    [[ -n "$ONLY_PROJECT" && "$name" \!= "$ONLY_PROJECT" ]] && continue
    run_detekt "$name" "$src_dirs" &
    pids+=($\!)
  done < <(grep -v '^#' "$CONF" | grep -v '^[[:space:]]*$')

  # Wait for all and collect exit codes
  local failed=0
  for pid in "${pids[@]}"; do
    wait "$pid" || ((failed++)) || true
  done

  echo ""
  echo "Analysis complete. Failed: $failed / ${#pids[@]}"
}

# ── Report ─────────────────────────────────────────────────────────────────────

generate_report() {
  local report_file="$REPORTS_DIR/REPORT.md"
  echo "→ Generating report..."
  python3 "$SCRIPT_DIR/report.py" "$REPORTS_DIR" > "$report_file"
  echo "✓ Report written to: $report_file"
}

# ── Main ───────────────────────────────────────────────────────────────────────

bootstrap
clone_projects
analyze_projects
generate_report

echo ""
echo "══════════════════════════════════════"
echo " Field test complete"
echo " Report: $REPORTS_DIR/REPORT.md"
echo "══════════════════════════════════════"
