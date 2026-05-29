# Start IPED Development Environment - React UI + ConfigurationServer API

Write-Host "IPED Configuration Platform - Development Environment" -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""

# Check if running as admin (helpful for ports)
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "Note: Running as non-admin. If port binding fails, run as administrator." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Starting services..." -ForegroundColor Green
Write-Host ""

$javaHome = $env:JAVA_HOME
if (-not $javaHome) {
    Write-Host "ERROR: JAVA_HOME environment variable not set" -ForegroundColor Red
    Write-Host "Please set JAVA_HOME to your Java 16+ installation" -ForegroundColor Yellow
    exit 1
}

$javaExe = Join-Path $javaHome "bin" "java.exe"

# Find the API jar
$apiJar = "L:\Workspace\IPED\iped-engine\target\iped-config-api-4.4.0-SNAPSHOT.jar"
$regularJar = "L:\Workspace\IPED\iped-engine\target\iped-engine-4.4.0-SNAPSHOT.jar"

if (Test-Path $apiJar) {
    $jar = $apiJar
    Write-Host "✓ Using API-only JAR" -ForegroundColor Green
} elseif (Test-Path $regularJar) {
    $jar = $regularJar
    Write-Host "✓ Using full JAR" -ForegroundColor Green
} else {
    Write-Host "✗ No JAR found!" -ForegroundColor Red
    Write-Host "  Please build with: mvn clean package -P api-only -DskipTests" -ForegroundColor Yellow
    exit 1
}

# Start ConfigurationServer
Write-Host ""
Write-Host "1. Starting ConfigurationServer on http://localhost:8080/api/v1" -ForegroundColor Cyan
$serverProcess = Start-Process -FilePath $javaExe `
    -ArgumentList "-cp", $jar, "iped.engine.config.api.ConfigurationServer", "8080" `
    -PassThru `
    -WindowStyle Hidden

Write-Host "   ✓ Process started (PID: $($serverProcess.Id))" -ForegroundColor Green

Write-Host ""
Write-Host "2. Waiting for API to be ready..." -ForegroundColor Cyan
Start-Sleep -Seconds 5

# Test API
$apiReady = $false
for ($i = 1; $i -le 5; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/schemas" -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            Write-Host "   ✓ API is responding!" -ForegroundColor Green
            $apiReady = $true
            break
        }
    } catch {
        if ($i -lt 5) {
            Start-Sleep -Seconds 1
        }
    }
}

if (-not $apiReady) {
    Write-Host "   ⚠ API not responding yet (may still be initializing)" -ForegroundColor Yellow
}

# Start React dev server
Write-Host ""
Write-Host "3. Starting React Development Server on http://localhost:3000" -ForegroundColor Cyan
Write-Host "   ✓ React will start in your default browser" -ForegroundColor Green
Write-Host ""
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "Services will be available at:" -ForegroundColor Green
Write-Host "  • React UI:      http://localhost:3000" -ForegroundColor White
Write-Host "  • Configuration API: http://localhost:8080/api/v1" -ForegroundColor White
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Press Ctrl+C to stop all services" -ForegroundColor Yellow
Write-Host ""

cd "L:\Workspace\IPED\iped-ui"
npm start

# Cleanup
Write-Host ""
Write-Host "Stopping services..." -ForegroundColor Yellow
Stop-Process -Id $serverProcess.Id -Force -ErrorAction SilentlyContinue
Write-Host "✓ ConfigurationServer stopped" -ForegroundColor Green
Write-Host "✓ Development environment closed" -ForegroundColor Green
