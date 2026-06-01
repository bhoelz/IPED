#!/usr/bin/env python3
"""Check for Maven and npm dependency updates across the project."""
import os
import xml.etree.ElementTree as ET
import json
import urllib.request
import urllib.error

NS = {"mvn": "http://maven.apache.org/POM/4.0.0"}

def find_poms(root):
    for dirpath, _, filenames in os.walk(root):
        for f in filenames:
            if f == "pom.xml":
                yield os.path.join(dirpath, f)

def parse_pom(path):
    try:
        tree = ET.parse(path)
        root = tree.getroot()
    except ET.ParseError as e:
        print(f"<!-- ParseError {path}: {e} -->")
        return [], {}

    props = {}
    prop_node = root.find("mvn:properties", NS)
    if prop_node is not None:
        for child in prop_node:
            tag = child.tag.split("}")[-1] if "}" in child.tag else child.tag
            props[tag] = (child.text or "").strip()

    deps = []
    for dep in root.findall(".//mvn:dependency", NS):
        def g(tag):
            el = dep.find(f"mvn:{tag}", NS)
            return (el.text or "").strip() if el is not None else ""
        group = g("groupId")
        artifact = g("artifactId")
        version = g("version")
        scope = g("scope")
        if not group or not artifact:
            continue
        # resolve property placeholders
        if version.startswith("${") and version.endswith("}"):
            key = version[2:-1]
            version = props.get(key, version)
        deps.append((group, artifact, version, scope or "compile", os.path.relpath(path)))
    return deps, props

def latest_version(group, artifact):
    """Query Maven Central REST API for latest release version."""
    q = f"g:{group}+AND+a:{artifact}"
    url = f"https://search.maven.org/solrsearch/select?q={q}&rows=1&wt=json"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            docs = data.get("response", {}).get("docs", [])
            if docs:
                return docs[0].get("latestVersion") or docs[0].get("v")
    except (urllib.error.URLError, urllib.error.HTTPError, json.JSONDecodeError, KeyError) as e:
        pass
    return None

def main():
    root = os.path.dirname(os.path.abspath(__file__))
    all_deps = []
    for pom in find_poms(root):
        # skip any target/ directories
        if "target" in pom.split(os.sep):
            continue
        deps, props = parse_pom(pom)
        all_deps.extend(deps)

    # deduplicate
    unique = {}
    for group, artifact, version, scope, source in all_deps:
        if not version or version.startswith("${"):
            continue
        key = (group, artifact)
        if key not in unique:
            unique[key] = (version, scope, source)

    print("| Module | Dependency | Current | Latest | Updatable |")
    print("|--------|------------|---------|--------|-----------|")

    # Npm deps already known
    npm = {
        "iped-ui": {
            "@rjsf/core": ("5.24.13", "6.6.1"),
            "@types/react": ("18.3.29", "19.2.15"),
            "@types/react-dom": ("18.3.7", "19.2.3"),
            "react": ("18.3.1", "19.2.6"),
            "react-dom": ("18.3.1", "19.2.6"),
        },
        "iped-webui": {
            "@angular/build": ("21.2.11", "21.2.13"),
            "@angular/cli": ("21.2.11", "21.2.13"),
            "@angular/common": ("21.2.13", "21.2.15"),
            "@angular/compiler": ("21.2.13", "21.2.15"),
            "@angular/compiler-cli": ("21.2.13", "21.2.15"),
            "@angular/core": ("21.2.13", "21.2.15"),
            "@angular/forms": ("21.2.13", "21.2.15"),
            "@angular/platform-browser": ("21.2.13", "21.2.15"),
            "@angular/router": ("21.2.13", "21.2.15"),
            "@openapitools/openapi-generator-cli": ("2.33.1", "2.34.0"),
            "jsdom": ("28.1.0", "29.1.1"),
            "typescript": ("5.9.3", "6.0.3"),
            "vitest": ("4.1.6", "4.1.7"),
        }
    }

    for mod, deps in npm.items():
        for dep, (cur, latest) in deps.items():
            updatable = "✅" if cur != latest else ""
            print(f"| {mod} | `{dep}` | {cur} | {latest} | {updatable} |")

    print()
    print("### Maven Dependencies")
    print()

    checked = 0
    for (group, artifact), (version, scope, source) in sorted(unique.items(), key=lambda x: x[0]):
        if scope == "test" and artifact.startswith("junit"):
            continue  # skip test-only to keep output manageable; still can be included
        latest = latest_version(group, artifact)
        checked += 1
        if latest and latest != version:
            print(f"| `{group}:{artifact}` | {version} | {latest} | ✅ |")
        else:
            print(f"| `{group}:{artifact}` | {version} | {latest or 'N/A'} | |")
        if checked > 150:
            break

if __name__ == "__main__":
    main()
