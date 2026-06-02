import subprocess
import os
import sys

cwd = "L:/Workspace/IPED"
pom = os.path.join(cwd, "iped-api", "pom.xml")
out_file = os.path.join(cwd, "maven-log.txt")

print(f"Working dir: {cwd}", flush=True)
print(f"POM: {pom}", flush=True)
print(f"Output: {out_file}", flush=True)

result = subprocess.run(
    ["mvn", "-f", pom, "clean", "test", "jacoco:report"],
    capture_output=True, text=True, cwd=cwd
)

content = result.stdout + result.stderr
with open(out_file, "w", encoding="utf-8") as f:
    f.write(content)
    f.write(f"\n\nEXIT CODE: {result.returncode}")

print(f"Written {len(content)} chars to {out_file}", flush=True)
print(f"Exit code: {result.returncode}", flush=True)

# Also print last 80 lines
lines = content.strip().split("\n")
for line in lines[-80:]:
    print(line, flush=True)