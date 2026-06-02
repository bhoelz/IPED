<#
.SYNOPSIS
    Start iped-app in dev mode using the locally compiled build.

.DESCRIPTION
    Locates the built release under target/release/, optionally triggers a fast
    Maven package (skipping tests) to refresh the JARs, then launches Bootstrap
    via 'java -jar iped.jar'.

    Use -Build to recompile iped-app and all upstream modules before launching.
    Use -Debug to attach a remote debugger on the forked child process (JDWP).

    iped arguments (-d, -o, -profile, etc.) are passed through to iped.jar.

.PARAMETER d
    Input data source (folder, image, .iped file…). Can be repeated: -d src1 -d src2.

.PARAMETER o
    Output / case folder.

.PARAMETER r
    Open an existing case folder in search UI mode.

.PARAMETER profile
    Processing profile (e.g. forensic, pedo, fastmode, blind, triage).

.PARAMETER log
    Redirect log to a specific file.

.PARAMETER nogui
    Text-mode processing (no progress window).

.PARAMETER nologfile
    Log to stdout instead of a log file.

.PARAMETER append
    Add data to an existing case (--append).

.PARAMETER Continue
    Continue a stopped or aborted processing (--continue).

.PARAMETER restart
    Discard last aborted processing and restart (--restart).

.PARAMETER IpedHome
    Path to the built IPED release directory.
    Defaults to <repo-root>/target/release/iped-<version from pom.xml>.

.PARAMETER Build
    Run 'mvn package -pl iped-app -am -DskipTests -q' before launching.

.PARAMETER Jdwp
    Enable JDWP on the Bootstrap process.
    Bootstrap increments the port by 1, so the child process listens on JdwpPort+1.

.PARAMETER JdwpPort
    JDWP port for the Bootstrap process (child gets JdwpPort+1). Default: 5005.

.PARAMETER Xmx
    Heap size forwarded to iped.jar (e.g. '4g', '8192m').

.EXAMPLE
    .\start-iped-app.ps1

.EXAMPLE
    .\start-iped-app.ps1 -Build -d C:\temp -o H:\output

.EXAMPLE
    .\start-iped-app.ps1 -d C:\evidence -o H:\case --append

.EXAMPLE
    .\start-iped-app.ps1 -Jdwp -JdwpPort 5005 -d C:\temp -o H:\output
#>
[CmdletBinding()]
param(
    # ---- iped arguments ----
    [Alias('data')]
    [string[]]$d,

    [Alias('output')]
    [string]$o,

    [string]$r,

    [string]$profile,

    [string]$log,

    [switch]$nogui,
    [switch]$nologfile,
    [switch]$append,
    [switch]$Continue,
    [switch]$restart,
    [switch]$portable,

    # ---- dev/launcher options ----
    [string]$IpedHome  = '',
    [switch]$Build,
    [switch]$Jdwp,
    [int]$JdwpPort     = 5005,
    [string]$Xmx       = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

function Resolve-IpedHome {
    if ($IpedHome) { return $IpedHome }

    $pom = Join-Path $PSScriptRoot 'pom.xml'
    if (-not (Test-Path $pom)) {
        throw "Cannot find pom.xml at $pom. Pass -IpedHome explicitly."
    }
    $version   = ([xml](Get-Content $pom -Raw)).project.version
    $candidate = Join-Path $PSScriptRoot "target\release\iped-$version"
    if (-not (Test-Path (Join-Path $candidate 'iped.jar'))) {
        throw @"
Release not found at: $candidate
Build the project first (or pass -Build):
    mvn package -pl iped-app -am -DskipTests -q
"@
    }
    return $candidate
}

function Get-JavaMajorVersion {
    param([string]$JavaBin)
    try {
        $raw = & $JavaBin -version 2>&1 | Select-Object -First 1
        if ($raw -match '"(\d+)') { return [int]$Matches[1] }
    } catch { }
    return 0
}

function Resolve-Java {
    param([string]$IpedHome)

    $candidates = @()
    if ($env:JAVA_HOME) {
        $candidates += Join-Path $env:JAVA_HOME 'bin\java.exe'
    }
    $candidates += 'java'
    $candidates += (Join-Path $IpedHome 'jre\bin\java.exe')

    foreach ($candidate in $candidates) {
        if ($candidate -ne 'java' -and -not (Test-Path $candidate -ErrorAction SilentlyContinue)) {
            continue
        }
        $major = Get-JavaMajorVersion -JavaBin $candidate
        if ($major -ge 16) {
            Write-Host "      Java $major found: $candidate"
            return $candidate
        }
        if ($major -gt 0) {
            Write-Warning "Skipping $candidate (Java $major < 16 required)."
        }
    }
    throw "No Java >= 16 found. Set JAVA_HOME or add a suitable JDK to PATH."
}

# ---------------------------------------------------------------------------
# Optional rebuild
# ---------------------------------------------------------------------------

if ($Build) {
    Write-Host '[0/2] Building iped-app (mvn package -pl iped-app -am -DskipTests -q)...'
    $mvn = if (Get-Command mvn -ErrorAction SilentlyContinue) { 'mvn' } else {
        $m = Join-Path $env:MAVEN_HOME 'bin\mvn.cmd'
        if (Test-Path $m) { $m } else { throw 'mvn not found. Set MAVEN_HOME or add Maven to PATH.' }
    }
    $proc = Start-Process -FilePath $mvn `
                          -ArgumentList @('package', '-pl', 'iped-app', '-am', '-DskipTests', '-q') `
                          -WorkingDirectory $PSScriptRoot `
                          -NoNewWindow `
                          -PassThru `
                          -Wait
    if ($proc.ExitCode -ne 0) {
        throw "Maven build failed (exit $($proc.ExitCode))."
    }
    Write-Host '      Build succeeded.'
}

# ---------------------------------------------------------------------------
# Resolve paths
# ---------------------------------------------------------------------------

$resolvedHome = Resolve-IpedHome
$java         = Resolve-Java -IpedHome $resolvedHome
$ipedJar      = Join-Path $resolvedHome 'iped.jar'

Write-Host ''
Write-Host '=== iped-app (dev mode) ==='
Write-Host "  IPED home : $resolvedHome"
Write-Host "  Java      : $java"
if ($Jdwp) {
    Write-Host "  Debug     : Bootstrap JDWP port $JdwpPort (child gets $($JdwpPort + 1))"
}
Write-Host ''

# ---------------------------------------------------------------------------
# Build argument list
# ---------------------------------------------------------------------------

$jvmArgs = [System.Collections.Generic.List[string]]::new()
if ($Jdwp) {
    $jvmArgs.Add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$JdwpPort")
}

$ipedArgs = [System.Collections.Generic.List[string]]::new()

foreach ($src in $d) {
    $ipedArgs.Add('-d')
    $ipedArgs.Add($src)
}
if ($o)       { $ipedArgs.Add('-o');       $ipedArgs.Add($o)       }
if ($r)       { $ipedArgs.Add('-r');       $ipedArgs.Add($r)       }
if ($profile) { $ipedArgs.Add('-profile'); $ipedArgs.Add($profile) }
if ($log)     { $ipedArgs.Add('-log');     $ipedArgs.Add($log)     }
if ($Xmx)     { $ipedArgs.Add("-Xmx$Xmx")                         }
if ($nogui)   { $ipedArgs.Add('--nogui')                           }
if ($nologfile) { $ipedArgs.Add('--nologfile')                     }
if ($append)  { $ipedArgs.Add('--append')                          }
if ($Continue){ $ipedArgs.Add('--continue')                        }
if ($restart) { $ipedArgs.Add('--restart')                         }
if ($portable){ $ipedArgs.Add('--portable')                        }

$launchArgs = [System.Collections.Generic.List[string]]::new()
$launchArgs.AddRange($jvmArgs)
$launchArgs.Add('-jar')
$launchArgs.Add($ipedJar)
$launchArgs.AddRange($ipedArgs)

# ---------------------------------------------------------------------------
# Launch
# ---------------------------------------------------------------------------

Write-Host '[1/2] Launching Bootstrap...'
Write-Host "      $java $($launchArgs -join ' ')"
Write-Host ''

$proc = Start-Process -FilePath $java `
                      -ArgumentList $launchArgs `
                      -WorkingDirectory $resolvedHome `
                      -NoNewWindow `
                      -PassThru `
                      -Wait

Write-Host ''
if ($proc.ExitCode -eq 0) {
    Write-Host '[2/2] iped-app exited normally.' -ForegroundColor Green
} else {
    Write-Warning "[2/2] iped-app exited with code $($proc.ExitCode)."
}
