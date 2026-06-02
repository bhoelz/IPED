import os
import glob

target = "iped-api/target"
result = []

# Check target contents
if os.path.isdir(target):
    for item in os.listdir(target):
        result.append("  " + item)
        full = os.path.join(target, item)
        if os.path.isdir(full):
            for sub in os.listdir(full):
                result.append("    " + sub)
                sf = os.path.join(full, sub)
                if os.path.isdir(sf):
                    for s2 in os.listdir(sf):
                        result.append("      " + s2)

# Check jacoco exec
exec_file = os.path.join(target, "jacoco.exec")
result.append("\njacoco.exec exists: " + str(os.path.isfile(exec_file)))

# Check surefire
surefire = os.path.join(target, "surefire-reports")
if os.path.isdir(surefire):
    xmls = glob.glob(os.path.join(surefire, "*.xml"))
    result.append("surefire XMLs: " + str(len(xmls)))
    for xf in xmls:
        with open(xf, "r", encoding="utf-8") as f:
            for line in f:
                if "tests=" in line and "testsuite" in line.lower():
                    result.append("  " + os.path.basename(xf) + ": " + line.strip()[:200])
                    break
else:
    result.append("surefire-reports: NOT FOUND")

# Check jacoco site
site = os.path.join(target, "site", "jacoco")
if os.path.isdir(site):
    result.append("\njacoco site files:")
    for f in os.listdir(site):
        result.append("  " + f)
    csv = os.path.join(site, "jacoco.csv")
    if os.path.isfile(csv):
        result.append("\njacoco.csv:")
        with open(csv, "r") as f:
            result.append(f.read())
else:
    result.append("\njacoco site: NOT FOUND")

# Write output
with open("analysis-output.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(result))

print("Done - wrote analysis-output.txt")