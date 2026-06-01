import subprocess
import os

cwd = "L:/Workspace/IPED"
out_file = os.path.join(cwd, "compile-check-result.txt")

result = subprocess.run(
    ["mvn", "-f", "iped-api/pom.xml", "test-compile", "-e"],
    capture_output=True, text=True, cwd=cwd
)

content = result.stdout + result.stderr
with open(out_file, "w", encoding="utf-8") as f:
    f.write("Exit code: " + str(result.returncode) + "\n\n")
    for line in content.split("\n"):
        if any(kw in line.lower() for kw in ["error", "warn", "compil", "fail", "success"]):
            f.write(line + "\n")
    f.write("\n\nFull surefire lines:\n")
    for line in content.split("\n"):
        if "Tests run" in line or "Running" in line or "test-classes" in line or "source file" in line:
            f.write(line + "\n")

print("Done")