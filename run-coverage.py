import subprocess
import os
import sys

cwd = "L:/Workspace/IPED"
log_file = os.path.join(cwd, "coverage-run.log")

# Run Maven from root pom targeting only iped-api
result = subprocess.run(
    ["mvn", "-pl", "iped-api", "test", "jacoco:report", "-Dmaven.test.failure.ignore=true"],
    capture_output=True, text=True, cwd=cwd
)

content = result.stdout + result.stderr
with open(log_file, "w", encoding="utf-8") as f:
    f.write(content)

# Write diagnostic info
diag_file = os.path.join(cwd, "coverage-diag.txt")
with open(diag_file, "w", encoding="utf-8") as f:
    f.write("Exit code: " + str(result.returncode) + "\n")
    f.write("Output length: " + str(len(content)) + "\n\n")
    
    # Check target directory
    target_dir = os.path.join(cwd, "iped-api", "target")
    f.write("Target dir exists: " + str(os.path.isdir(target_dir)) + "\n")
    if os.path.isdir(target_dir):
        for item in os.listdir(target_dir):
            f.write("  " + item + "\n")
            full = os.path.join(target_dir, item)
            if os.path.isdir(full):
                for sub in os.listdir(full):
                    f.write("    " + sub + "\n")
                    sub_full = os.path.join(full, sub)
                    if os.path.isdir(sub_full):
                        for sub2 in os.listdir(sub_full):
                            f.write("      " + sub2 + "\n")
    
    # Check jacoco
    jacoco_exec = os.path.join(target_dir, "jacoco.exec")
    f.write("\njacoco.exec exists: " + str(os.path.isfile(jacoco_exec)) + "\n")
    if os.path.isfile(jacoco_exec):
        f.write("jacoco.exec size: " + str(os.path.getsize(jacoco_exec)) + "\n")
    
    site = os.path.join(target_dir, "site", "jacoco")
    f.write("\nsite/jacoco exists: " + str(os.path.isdir(site)) + "\n")
    if os.path.isdir(site):
        f.write("site/jacoco contents: " + str(os.listdir(site)) + "\n")
        csv = os.path.join(site, "jacoco.csv")
        if os.path.isfile(csv):
            f.write("\njacoco.csv contents:\n")
            with open(csv, "r") as csvf:
                f.write(csvf.read())
        xml = os.path.join(site, "jacoco.xml")
        if os.path.isfile(xml):
            f.write("\njacoco.xml exists, size: " + str(os.path.getsize(xml)) + "\n")
    
    # Surefire reports
    surefire = os.path.join(target_dir, "surefire-reports")
    f.write("\nsurefire-reports exists: " + str(os.path.isdir(surefire)) + "\n")
    if os.path.isdir(surefire):
        xml_files = [f2 for f2 in os.listdir(surefire) if f2.endswith(".xml")]
        f.write("surefire XML files: " + str(len(xml_files)) + "\n")
        for xf in xml_files:
            f.write("  " + xf + "\n")

print("Done. Check coverage-diag.txt")