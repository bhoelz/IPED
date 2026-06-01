import subprocess
import os

cwd = "L:/Workspace/IPED"
out_file = os.path.join(cwd, "iped-api-full-run.log")

result = subprocess.run(
    ["mvn", "-f", "iped-api/pom.xml", "clean", "test", "jacoco:report", "-Dmaven.test.failure.ignore=true"],
    capture_output=True, text=True, cwd=cwd
)

content = result.stdout + result.stderr
with open(out_file, "w", encoding="utf-8") as f:
    f.write("Exit code: " + str(result.returncode) + "\n\n")
    f.write(content)

print("Wrote " + str(len(content)) + " chars to " + out_file)