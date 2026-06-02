# Start IPED ConfigurationServer API

Write-Host "IPED Configuration API Server Launcher" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

$javaHome = $env:JAVA_HOME
if (-not $javaHome) {
    Write-Host "ERROR: JAVA_HOME environment variable not set" -ForegroundColor Red
    exit 1
}

$javaExe = Join-Path $javaHome "bin" "java.exe"
if (-not (Test-Path $javaExe)) {
    Write-Host "ERROR: Java executable not found at $javaExe" -ForegroundColor Red
    exit 1
}

# Find the API jar
$apiJar = "L:\Workspace\IPED\iped-engine\target\iped-config-api-4.4.0-SNAPSHOT.jar"
$regularJar = "L:\Workspace\IPED\iped-engine\target\iped-engine-4.4.0-SNAPSHOT.jar"

if (Test-Path $apiJar) {
    $jar = $apiJar
    Write-Host "Using: API-only JAR (iped-config-api-4.4.0-SNAPSHOT.jar)" -ForegroundColor Green
} elseif (Test-Path $regularJar) {
    $jar = $regularJar
    Write-Host "Using: Full JAR (iped-engine-4.4.0-SNAPSHOT.jar)" -ForegroundColor Yellow
} else {
    Write-Host "ERROR: No JAR found. Please build with: mvn clean package" -ForegroundColor Red
    Write-Host "  - Full build:     mvn clean package -DskipTests" -ForegroundColor Gray
    Write-Host "  - API-only build: mvn clean package -P api-only -DskipTests" -ForegroundColor Gray
    exit 1
}

Write-Host "Java: $javaExe" -ForegroundColor Gray
Write-Host "JAR: $(Split-Path -Leaf $jar)" -ForegroundColor Gray
Write-Host ""

# Start server
Write-Host "Starting ConfigurationServer on http://localhost:8080/api/v1" -ForegroundColor Green
Write-Host ""
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Yellow
Write-Host ""

# Run with proper Java arguments
$arguments = @(
    "-cp",
    $jar,
    "iped.engine.config.api.ConfigurationServer",
    "8080"
)

& $javaExe $arguments
