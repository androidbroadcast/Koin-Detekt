#\!/usr/bin/env python3
"""
Parse detekt XML reports and produce a Markdown field-test summary.

Usage: python3 report.py <reports-dir>
"""

import sys
import os
import xml.etree.ElementTree as ET
from collections import defaultdict
from datetime import date

PLUGIN_VERSION = "1.0.0"

# All 51 rule IDs — used to detect zero-firing rules
ALL_RULES = [
    # Service Locator
    "NoGetOutsideModuleDefinition", "NoInjectDelegate", "NoKoinComponentInterface",
    "NoGlobalContextAccess", "NoKoinGetInApplication",
    # Module DSL
    "EmptyModule", "SingleForNonSharedDependency", "MissingScopedDependencyQualifier",
    "DeprecatedKoinApi", "ModuleIncludesOrganization", "ModuleAsTopLevelVal",
    "ExcessiveCreatedAtStart", "OverrideInIncludedModule", "ConstructorDslAmbiguousParameters",
    "ParameterTypeMatchesReturnType", "GenericDefinitionWithoutQualifier",
    "DuplicateBindingWithoutQualifier", "EnumQualifierCollision", "UnassignedQualifierInWithOptions",
    # Scope Management
    "MissingScopeClose", "ScopedDependencyOutsideScopeBlock", "ViewModelAsSingleton",
    "CloseableWithoutOnClose", "ScopeAccessInOnDestroy", "FactoryInScopeBlock",
    "KtorRequestScopeMisuse", "ScopeDeclareWithActivityOrFragment",
    # Platform
    "AndroidContextNotFromKoin", "StartKoinInActivity", "ActivityFragmentKoinScope",
    "KoinViewModelOutsideComposable", "KoinInjectInPreview", "RememberKoinModulesLeak",
    "KtorApplicationKoinInit", "KtorRouteScopeMisuse",
    # Architecture
    "LayerBoundaryViolation", "PlatformImportRestriction", "CircularModuleDependency",
    "GetConcreteTypeInsteadOfInterface",
    # Koin Annotations
    "MixingDslAndAnnotations", "MissingModuleAnnotation", "ConflictingBindings",
    "ScopedWithoutQualifier", "AnnotationProcessorNotConfigured", "SingleAnnotationOnObject",
    "ViewModelAnnotatedAsSingle", "TooManyInjectedParams", "InvalidNamedQualifierCharacters",
    "KoinAnnotationOnExtensionFunction", "AnnotatedClassImplementsNestedInterface",
    "InjectedParamWithNestedGenericType",
]


def parse_report(xml_path: str) -> dict[str, list[dict]]:
    """Parse detekt XML. Returns {rule_id: [{"file": ..., "line": ..., "message": ...}]}"""
    findings: dict[str, list[dict]] = defaultdict(list)
    try:
        tree = ET.parse(xml_path)
        root = tree.getroot()
        for file_elem in root.findall("file"):
            filepath = file_elem.get("name", "")
            for error in file_elem.findall("error"):
                source = error.get("source", "")
                rule_id = source.split(".")[-1] if "." in source else source
                findings[rule_id].append({
                    "file": filepath,
                    "line": error.get("line", "?"),
                    "message": error.get("message", ""),
                })
    except ET.ParseError as e:
        print(f"  Warning: could not parse {xml_path}: {e}", file=sys.stderr)
    return findings


def main():
    if len(sys.argv) < 2:
        print("Usage: report.py <reports-dir>", file=sys.stderr)
        sys.exit(1)

    reports_dir = sys.argv[1]
    xml_files = sorted(f for f in os.listdir(reports_dir) if f.endswith(".xml"))

    if not xml_files:
        print(f"No XML files found in {reports_dir}", file=sys.stderr)
        sys.exit(1)

    projects = [f[:-4] for f in xml_files]  # strip .xml
    all_findings: dict[str, dict[str, list]] = {}  # project → rule → findings

    for xml_file in xml_files:
        name = xml_file[:-4]
        path = os.path.join(reports_dir, xml_file)
        all_findings[name] = parse_report(path)

    # ── Header ─────────────────────────────────────────────────────────────────
    print(f"# detekt-koin {PLUGIN_VERSION} — Field Test Report")
    print(f"\n**Date:** {date.today()}  ")
    print(f"**Projects analyzed:** {len(projects)}  ")
    total = sum(sum(len(v) for v in f.values()) for f in all_findings.values())
    print(f"**Total findings:** {total}\n")

    # ── Summary table ──────────────────────────────────────────────────────────
    print("## Summary\n")
    print("| Project | Issues | Rules fired | Status |")
    print("|---------|--------|-------------|--------|")
    for name in projects:
        findings = all_findings[name]
        issue_count = sum(len(v) for v in findings.values())
        rules_fired = len(findings)
        status = "✅" if issue_count == 0 else "⚠️"
        print(f"| {name} | {issue_count} | {rules_fired} | {status} |")

    # ── Top rules by firing count ──────────────────────────────────────────────
    rule_totals: dict[str, dict[str, int]] = defaultdict(lambda: defaultdict(int))
    for name, findings in all_findings.items():
        for rule_id, issues in findings.items():
            rule_totals[rule_id][name] = len(issues)

    if rule_totals:
        print("\n## Top Rules by Firing Count\n")
        sorted_rules = sorted(rule_totals.items(), key=lambda x: -sum(x[1].values()))

        header = "| Rule | Total | " + " | ".join(projects) + " |"
        separator = "|------|-------|" + "|".join(["-------"] * len(projects)) + "|"
        print(header)
        print(separator)
        for rule_id, per_project in sorted_rules:
            total_rule = sum(per_project.values())
            cols = " | ".join(str(per_project.get(p, "—")) for p in projects)
            print(f"| {rule_id} | {total_rule} | {cols} |")

    # ── Zero-firing rules ──────────────────────────────────────────────────────
    fired_rules = set(rule_totals.keys())
    zero_rules = [r for r in ALL_RULES if r not in fired_rules]

    print("\n## Rules With Zero Firings\n")
    print(f"**{len(zero_rules)} of {len(ALL_RULES)} rules did not fire across any project.**\n")
    print("These are candidates for investigation: either the projects don't have these patterns,")
    print("or the rules may have detection gaps.\n")

    for rule in zero_rules:
        print(f"- `{rule}`")


if __name__ == "__main__":
    main()
