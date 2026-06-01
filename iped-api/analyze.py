import os
import glob

target = os.path.join("L:", os.sep, "Workspace", "IPED", "iped-api", "target")
out_file = os.path.join("L:", os.sep, "Workspace", "IPED", "iped-api", "ANALYSIS.txt")

result = []

exec_file = os.path.join(target, "jacoco.exec")
result.append("jacoco.exec exists: " + str(os.path.isfile(exec_file)))

surefire = os.path.join(target, "surefire-reports")
if os.path.isdir(surefire):
    xmls = glob.glob(os.path.join(surefire, "*.xml"))
    result.append("surefire XMLs: " + str(len(xmls)))
    for xf in xmls:
        with open(xf, "r", encoding="utf-8") as f:
            for line in f:
                if "tests=" in line:
                    result.append("  " + os.path.basename(xf) + ": " + line.strip()[:200])
                    break
else:
    result.append("surefire-reports: NOT FOUND")

site = os.path.join(target, "site", "jacoco")
if os.path.isdir(site):
    result.append("jacoco site files: " + str(os.listdir(site)))
    csv = os.path.join(site, "jacoco.csv")
    if os.path.isfile(csv):
        result.append("jacoco.csv:")
        with open(csv, "r") as f:
            result.append(f.read())
else:
    result.append("jacoco site: NOT FOUND")

with open(out_file, "w", encoding="utf-8") as f:
    f.write("\n".join(result))

print("Done")