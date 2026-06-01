import os
import glob

target = "iped-api/target"
surefire = os.path.join(target, "surefire-reports")

lines = []

# List all surefire report files
if os.path.isdir(surefire):
    txts = glob.glob(os.path.join(surefire, "*.txt"))
    lines.append("Surefire report files: " + str(len(txts)))
    for t in txts:
        basename = os.path.basename(t)
        with open(t, "r", encoding="utf-8") as f:
            content = f.read()
        # Extract test count from first line
        first_line = content.split("\n")[0] if content else ""
        lines.append("  " + basename + ": " + first_line[:100])

    xmls = glob.glob(os.path.join(surefire, "*.xml"))
    lines.append("\nSurefire XML files: " + str(len(xmls)))
else:
    lines.append("surefire-reports NOT FOUND")

# Check jacoco
exec_f = os.path.join(target, "jacoco.exec")
lines.append("\njacoco.exec: " + ("EXISTS" if os.path.isfile(exec_f) else "NOT FOUND"))
site = os.path.join(target, "site", "jacoco")
if os.path.isdir(site):
    csv = os.path.join(site, "jacoco.csv")
    if os.path.isfile(csv):
        lines.append("jacoco.csv contents:")
        with open(csv, "r") as f:
            lines.append(f.read())
    else:
        lines.append("jacoco.csv NOT FOUND")
else:
    lines.append("jacoco site NOT FOUND")

# Write to iped-api directory (where write_to_file has permission)
out = os.path.join("iped-api", "VERIFY.txt")
with open(out, "w", encoding="utf-8") as f:
    f.write("\n".join(lines))