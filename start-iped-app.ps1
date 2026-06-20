<#
.SYNOPSIS
    Start iped-app in dev mode using the locally compiled build.

.DESCRIPTION
    Locates the built release under target/release/, optionally triggers a fast
    Maven package (skipping tests) to refresh the JARs, then launches Bootstrap
    via 'java -jar iped.jar'.

    Use -Compile to recompile iped-app and all upstream modules before launching.
    Use -Jdwp to attach a remote debugger on the forked child process.

    All real iped.jar arguments (-d, -o, -profile, -l, -p, -b, --portable, -X..., etc.)
    are forwarded verbatim. This script deliberately declares NO formal param() block
    at all and parses $args itself in a plain loop below, matching only a short list of
    exact, full-length names (-r, -IpedHome, -Compile, -Jdwp, -JdwpPort, -Xmx) before
    forwarding everything else untouched, in order. This is intentional and load-bearing:
      - Any [CmdletBinding()]/[Parameter(...)] attribute makes PowerShell an "advanced"
        script, which (a) abbreviation-matches "-x" tokens against every declared
        parameter name via unique prefix, and (b) exposes its own reserved common
        parameters (-Debug, -OutVariable, -OutBuffer, -ProgressAction, -PipelineVariable,
        etc.) — both silently swallow short iped flags ("-d"->-Debug, "-o" ambiguous
        with -OutVariable/-OutBuffer, "-p" ambiguous with -ProgressAction/-PipelineVariable).
      - Even a formal param() block with NO attributes (a "simple" script) still gets
        automatic *positional* binding in declaration order. When invoked via the call
        operator or a bare path from within a live PowerShell session (as opposed to
        `powershell -File`, which is how iped-runner's ExecutionService launches this
        script), any "-x" token PowerShell doesn't recognize as a parameter name is
        bound POSITIONALLY to the next unfilled parameter instead of being left alone —
        e.g. "-dname Evidence_001" silently landing on an unrelated positional slot.
    Manually walking $args sidesteps the parameter binder entirely, so behavior is
    identical regardless of how the script is invoked. Do NOT reintroduce a param()
    block or give this script a new option whose name could collide with an iped flag
    (current iped short flags: -d/-data, -dname, -o/-output, -remove, -l/-keywordlist,
    -ocr, -log, -nocontent, -tz/-timezone, -b/-blocksize, -p/-password, -profile,
    -splash, -X).

.PARAMETER r
    Open an existing case folder in search UI mode (dev convenience, not an iped.jar flag).

.PARAMETER IpedHome
    Path to the built IPED release directory.
    Defaults to <repo-root>/target/release/iped-<version from pom.xml>.

.PARAMETER Compile
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
    .\start-iped-app.ps1 -Compile -d C:\temp -o H:\output

.EXAMPLE
    .\start-iped-app.ps1 -d C:\evidence -o H:\case --append

.EXAMPLE
    .\start-iped-app.ps1 -Jdwp -JdwpPort 5005 -d C:\temp -o H:\output
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ---------------------------------------------------------------------------
# Manual argument parsing (see .DESCRIPTION for why this can't be a param() block)
# ---------------------------------------------------------------------------

$r               = $null
$IpedHome        = ''
$Compile         = $false
$Jdwp            = $false
$JdwpPort        = 5005
$Xmx             = ''
$passthroughArgs = [System.Collections.Generic.List[string]]::new()

$i = 0
while ($i -lt $args.Count) {
    switch ($args[$i]) {
        '-r'        { $i++; $r = $args[$i] }
        '-IpedHome' { $i++; $IpedHome = $args[$i] }
        '-Compile'  { $Compile = $true }
        '-Jdwp'     { $Jdwp = $true }
        '-JdwpPort' { $i++; $JdwpPort = [int]$args[$i] }
        '-Xmx'      { $i++; $Xmx = $args[$i] }
        default     { $passthroughArgs.Add($args[$i]) }
    }
    $i++
}

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
Build the project first (or pass -Compile):
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

if ($Compile) {
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

if ($r)   { $ipedArgs.Add('-r'); $ipedArgs.Add($r) }
if ($Xmx) { $ipedArgs.Add("-Xmx$Xmx") }
$ipedArgs.AddRange([string[]]$passthroughArgs)

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
