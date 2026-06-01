$ErrorActionPreference = "Continue"
$ipath = "L:\Workspace\IPED\iped-api"
$logfile = "L:\Workspace\IPED\iped-api-tests.log"

# Remove old site directory
Remove-Item -Recurse -Force "$ipath\target\site" -ErrorAction SilentlyContinue
Remove-Item -Force "$ipath\target\jacoco.exec" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "$ipath\target\surefire-reports" -ErrorAction SilentlyContinue

# Run Maven from module directory
Set-Location $ipath
$mvnOutput = & mvn clean test jacoco:report -Dmaven.test.failure.ignore=true 2>&1
$mvnOutput | Out-File -FilePath $logfile -Encoding utf8

# Add diagnostic info
$diag = "`n`n=== DIAGNOSTICS ===`n"
$diag += "Target contents: " + (Get-ChildItem "$ipath\target" -ErrorAction SilentlyContinue | ForEach-Object { $_.Name }) + "`n"
$diag += "jacoco.exec exists: " + (Test-Path "$ipath\target\jacoco.exec") + "`n"
$diag += "site exists: " + (Test-Path "$ipath\target\site\jacoco") + "`n"
if (Test-Path "$ipath\target\site\jacoco") {
    $diag += "site contents: " + (Get-ChildItem "$ipath\target\site\jacoco" | ForEach-Object { $_.Name }) + "`n"
}
$diag += "surefire exists: " + (Test-Path "$ipath\target\surefire-reports") + "`n"
if (Test-Path "$ipath\target\surefire-reports") {
    $diag += "surefire xml count: " + (Get-ChildItem "$ipath\target\surefire-reports\*.xml").Count + "`n"
}

Add-Content -Path $logfile -Value $diag -Encoding utf8

Write-Host "Log written to $logfile"
Write-Host $diag