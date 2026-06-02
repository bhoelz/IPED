import subprocess
import os

cwd = "L:/Workspace/IPED"
result = subprocess.run(
    ["mvn", "-f", "iped-api/pom.xml", "test-compile", "-e"],
    capture_output=True, text=True, cwd=cwd
)

lines = (result.stdout + result.stderr).split("\n")
with open("check-test-result.txt", "w", encoding="utf-8") as f:
    for line in lines:
        if "error" in line.lower() or "ERROR" in line or "compil" in line.lower() or "warning" in line.lower():
            f.write(line + "\n")
    f.write("\n\nExit code: " + str(result.returncode) + "\n")