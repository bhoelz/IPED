import subprocess
import os

cwd = "L:/Workspace/IPED"
result = subprocess.run(
    ["mvn", "-f", "iped-api/pom.xml", "test-compile", "-e"],
    capture_output=True, text=True, cwd=cwd
)

out_file = os.path.join(cwd, "compile-result.txt")
with open(out_file, "w", encoding="utf-8") as f:
    f.write("Exit: " + str(result.returncode) + "\n\n")
    content = result.stdout + result.stderr
    # Write all lines containing error/warning/compile keywords
    for line in content.split("\n"):
        stripped = line.strip()
        if any(kw in stripped.lower() for kw in ["error", "warn", "compile", "fail", "success", "IItem", "source file"]):
            f.write(stripped + "\n")

    # Also write the last 20 lines
    f.write("\n\n--- LAST 20 LINES ---\n")
    lines = content.strip().split("\n")
    for line in lines[-20:]:
        f.write(line + "\n")

print("Wrote to", out_file)