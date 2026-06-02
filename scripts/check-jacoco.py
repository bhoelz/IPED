import os

output = []
d = "iped-api/target"
items = os.listdir(d)
output.append("Target dir contents: " + str(items))

jacoco_exec = os.path.join(d, "jacoco.exec")
output.append("jacoco.exec exists: " + str(os.path.isfile(jacoco_exec)))

surefire = os.path.join(d, "surefire-reports")
output.append("surefire-reports exists: " + str(os.path.isdir(surefire)))

site = os.path.join(d, "site", "jacoco")
output.append("site/jacoco exists: " + str(os.path.isdir(site)))

if os.path.isdir(site):
    output.append("site/jacoco contents: " + str(os.listdir(site)))

with open("check-jacoco-output.txt", "w") as f:
    f.write("\n".join(output))