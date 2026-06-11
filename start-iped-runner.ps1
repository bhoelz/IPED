<#
.SYNOPSIS
    Start the iped-runner Spring Boot UI and open it in the default browser.

.DESCRIPTION
    Locates the built iped-runner JAR under iped-runner/target/, launches it with
    'java -jar', waits until the server responds on the configured port, then opens
    the default browser.  The script keeps the terminal attached to the process so
    Ctrl+C stops the server cleanly.

    Use -Build to recompile iped-runner before launching.
    Use -Dev   to pass -Drunner.dev=true (serves JS via Babel CDN, no Vite needed).
    Use -Jdwp  to attach a remote debugger.

.PARAMETER Port
    HTTP port the server listens on.  Overrides the value in application.yml (8092).

.PARAMETER Executable
    Path (or command) used by the runner to invoke IPED.
    Overrides 'runner.executable' in application.yml.

.PARAMETER Build
    Recompile iped-runner (and its deps) before launching.

.PARAMETER Dev
    Launch with -Drunner.dev=true so JS files are served without the Vite bundle.

.PARAMETER Jdwp
    Enable JDWP remote debugging.

.PARAMETER JdwpPort
    JDWP listen port. Default: 5010.

.PARAMETER NoBrowser
    Start the server but do not open the browser.

.EXAMPLE
    .\start-iped-runner.ps1

.EXAMPLE
    .\start-iped-runner.ps1 -Build -Dev

.EXAMPLE
    .\start-iped-runner.ps1 -Port 9000 -Executable "java -jar C:\iped\iped-app.jar"

.EXAMPLE
    .\start-iped-runner.ps1 -Jdwp -JdwpPort 5010
#>
[CmdletBinding()]
param(
    [int]   $Port       = 8092,
    [string]$Executable = '',
    [switch]$Build,
    [switch]$Dev,
    [switch]$Jdwp,
    [int]   $JdwpPort   = 5010,
    [switch]$NoBrowser
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

function Get-JavaMajorVersion {
    param([string]$JavaBin)
    try {
        $raw = & $JavaBin -version 2>&1 | Select-Object -First 1
        if ($raw -match '"(\d+)') { return [int]$Matches[1] }
    } catch { }
    return 0
}

function Resolve-Java {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += Join-Path $env:JAVA_HOME 'bin\java.exe' }
    $candidates += 'java'

    foreach ($candidate in $candidates) {
        if ($candidate -ne 'java' -and -not (Test-Path $candidate -ErrorAction SilentlyContinue)) { continue }
        $major = Get-JavaMajorVersion -JavaBin $candidate
        if ($major -ge 17) {
            Write-Host "      Java $major : $candidate"
            return $candidate
        }
        if ($major -gt 0) { Write-Warning "Skipping $candidate (Java $major < 17)." }
    }
    throw 'No Java >= 17 found. Set JAVA_HOME or add a suitable JDK to PATH.'
}

function Resolve-RunnerJar {
    $targetDir = Join-Path $PSScriptRoot 'iped-runner\target'
    $jar = Get-ChildItem -Path $targetDir -Filter 'iped-runner-*.jar' -ErrorAction SilentlyContinue |
           Where-Object { $_.Name -notmatch '-sources|-javadoc' } |
           Sort-Object LastWriteTime -Descending |
           Select-Object -First 1

    if (-not $jar) {
        throw @"
iped-runner JAR not found under $targetDir.
Build the project first (or use -Build):
    mvn package -pl iped-runner -am -DskipTests -q
"@
    }
    return $jar.FullName
}

function Wait-ForServer {
    param([int]$Port, [int]$TimeoutSec = 60)
    $url      = "http://localhost:$Port/actuator/health"
    $deadline = (Get-Date).AddSeconds($TimeoutSec)

    Write-Host -NoNewline '      Waiting for server'
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
            if ($r.StatusCode -eq 200) { Write-Host ' ready.'; return $true }
        } catch { }

        # Fallback: try the root page (actuator may not be on classpath)
        try {
            $r = Invoke-WebRequest -Uri "http://localhost:$Port/" -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
            if ($r.StatusCode -lt 500) { Write-Host ' ready.'; return $true }
        } catch { }

        Write-Host -NoNewline '.'
        Start-Sleep -Seconds 1
    }
    Write-Host ' timed out.'
    return $false
}

# ---------------------------------------------------------------------------
# Optional rebuild
# ---------------------------------------------------------------------------

if ($Build) {
    Write-Host '[1/3] Building iped-runner...'
    $mvn = if (Get-Command mvn -ErrorAction SilentlyContinue) { 'mvn' } else {
        $m = Join-Path $env:MAVEN_HOME 'bin\mvn.cmd'
        if (Test-Path $m) { $m } else { throw 'mvn not found. Set MAVEN_HOME or add Maven to PATH.' }
    }
    $proc = Start-Process -FilePath $mvn `
                          -ArgumentList @('package', '-pl', 'iped-runner', '-am', '-DskipTests', '-q') `
                          -WorkingDirectory $PSScriptRoot `
                          -NoNewWindow -PassThru -Wait
    if ($proc.ExitCode -ne 0) { throw "Maven build failed (exit $($proc.ExitCode))." }
    Write-Host '      Build succeeded.'
} else {
    Write-Host '[1/3] Skipping build (use -Build to recompile).'
}

# ---------------------------------------------------------------------------
# Resolve paths
# ---------------------------------------------------------------------------

$java      = Resolve-Java
$runnerJar = Resolve-RunnerJar

Write-Host ''
Write-Host '=== iped-runner ==='
Write-Host "  JAR  : $runnerJar"
Write-Host "  Java : $java"
Write-Host "  Port : $Port"
if ($Dev)  { Write-Host '  Mode : dev (Babel CDN, no Vite bundle)' }
if ($Jdwp) { Write-Host "  JDWP : port $JdwpPort" }
Write-Host ''

# ---------------------------------------------------------------------------
# Build JVM argument list
# ---------------------------------------------------------------------------

$jvmArgs = [System.Collections.Generic.List[string]]::new()

if ($Jdwp) {
    $jvmArgs.Add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$JdwpPort")
}

$jvmArgs.Add('-jar')
$jvmArgs.Add($runnerJar)
$jvmArgs.Add("--server.port=$Port")

if ($Dev)        { $jvmArgs.Add('-Drunner.dev=true') }
if ($Executable) { $jvmArgs.Add("--runner.executable=$Executable") }

# ---------------------------------------------------------------------------
# Launch server (background job so we can open the browser while it boots)
# ---------------------------------------------------------------------------

Write-Host '[2/3] Starting iped-runner...'

$proc = Start-Process -FilePath $java `
                      -ArgumentList $jvmArgs `
                      -WorkingDirectory $PSScriptRoot `
                      -NoNewWindow `
                      -PassThru

$url = "http://localhost:$Port"

$ready = Wait-ForServer -Port $Port -TimeoutSec 90

if (-not $ready) {
    Write-Warning "Server did not respond within 90 s. Opening browser anyway — it may take a moment."
}

# ---------------------------------------------------------------------------
# Open browser
# ---------------------------------------------------------------------------

if (-not $NoBrowser) {
    Write-Host "[3/3] Opening $url ..."
    Start-Process $url
} else {
    Write-Host "[3/3] -NoBrowser set; skipping. Navigate to $url manually."
}

Write-Host ''
Write-Host "Server running (PID $($proc.Id)). Press Ctrl+C to stop." -ForegroundColor Cyan

# Keep the terminal attached; forward Ctrl+C to the child process
try {
    $proc.WaitForExit()
} finally {
    if (-not $proc.HasExited) {
        Write-Host "`nStopping iped-runner..." -ForegroundColor Yellow
        $proc.Kill($true)   # Kill process tree
    }
}

Write-Host ''
if ($proc.ExitCode -eq 0) {
    Write-Host 'iped-runner stopped.' -ForegroundColor Green
} else {
    Write-Warning "iped-runner exited with code $($proc.ExitCode)."
}
