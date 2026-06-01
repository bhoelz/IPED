#!/usr/bin/env python3
"""Parse JaCoCo XML reports and emit a Markdown coverage table."""
import xml.etree.ElementTree as ET
import os
import glob

REPORTS = {
    "iped-api": "iped-api/target/site/jacoco/jacoco.xml",
    "iped-engine-core": "iped-engine-core/target/site/jacoco/jacoco.xml",
}

def parse_jacoco(path):
    if not os.path.exists(path):
        return None
    tree = ET.parse(path)
    root = tree.getroot()
    # counters are at the root <report> level
    counters = {}
    for counter in root.findall("counter"):
        ctype = counter.get("type")
        missed = int(counter.get("missed", 0))
        covered = int(counter.get("covered", 0))
        total = missed + covered
        pct = (covered / total * 100) if total else 0.0
        counters[ctype] = {"missed": missed, "covered": covered, "total": total, "pct": pct}
    return counters

def fmt_pct(pct):
    return f"{pct:.1f}%"

def main():
    rows = []
    for module, path in REPORTS.items():
        data = parse_jacoco(path)
        if data is None:
            rows.append((module, "N/A", "N/A", "N/A", "N/A", "N/A"))
            continue
        inst = data.get("INSTRUCTION", {})
        branch = data.get("BRANCH", {})
        line = data.get("LINE", {})
        method = data.get("METHOD", {})
        cls = data.get("CLASS", {})
        rows.append((
            module,
            fmt_pct(inst.get("pct", 0)),
            f"{inst.get('covered',0)}/{inst.get('total',0)}",
            fmt_pct(branch.get("pct", 0)),
            fmt_pct(line.get("pct", 0)),
            fmt_pct(method.get("pct", 0)),
        ))

    # Also try to read surefire test counts
    test_counts = {}
    for module in REPORTS.keys():
        surefire_dir = os.path.join(module, "target", "surefire-reports")
        if os.path.isdir(surefire_dir):
            total_tests = 0
            total_failures = 0
            total_errors = 0
            for xml_file in glob.glob(os.path.join(surefire_dir, "*.xml")):
                try:
                    tree = ET.parse(xml_file)
                    root = tree.getroot()
                    # Surefire XML root is <testsuite>
                    if root.tag == "testsuite":
                        total_tests += int(root.get("tests", 0))
                        total_failures += int(root.get("failures", 0))
                        total_errors += int(root.get("errors", 0))
                except Exception:
                    pass
            test_counts[module] = (total_tests, total_failures, total_errors)

    print("# Code Coverage Report")
    print("")
    print("Generated from JaCoCo XML reports after running `mvn test`.")
    print("")
    print("| Module | Tests | Instruction | Branch | Line | Method |")
    print("|--------|-------|-------------|--------|------|--------|")
    for module, inst_pct, inst_frac, branch_pct, line_pct, method_pct in rows:
        tests_info = test_counts.get(module, (0,0,0))
        tests_str = f"{tests_info[0]}"
        if tests_info[1] or tests_info[2]:
            tests_str += f" ({tests_info[1]}F/{tests_info[2]}E)"
        print(f"| {module} | {tests_str} | {inst_pct} | {branch_pct} | {line_pct} | {method_pct} |")

    print("")
    print("## Notes")
    print("- `iped-webapi` had a test failure (`ArchitectureImportsTest`), so its JaCoCo report was not generated.")
    print("- `iped-utils` and `iped-mcp` either had no tests or were skipped in this run.")
    print("- Coverage is calculated as `covered / (covered + missed)` for each metric.")

if __name__ == "__main__":
    main()
