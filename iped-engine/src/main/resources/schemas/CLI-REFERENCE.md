# IPED Command-Line Interface (CLI) Reference

This document provides comprehensive documentation for all IPED command-line interfaces and their arguments.

## Overview

IPED provides three primary command-line interfaces:

1. **IPED Processing Application** - Main forensic evidence processing tool
2. **IPED Web API Server** - RESTful API for case access
3. **IPED Search Application** - GUI for case analysis

## 1. IPED Processing Application

**Jar**: `iped.jar`
**Class**: `iped.app.processing.Main`
**Invocation**: `java -jar iped.jar [options]`

### Purpose

The IPED processing application is the main tool for digital forensic evidence processing. It:
- Imports forensic images and data sources
- Processes evidence with configurable task pipelines
- Indexes data for searchability
- Extracts metadata and artifacts
- Generates case reports

### Required Arguments

#### `-d, --data PATH` (Multiple allowed)

Input data sources to process. Can be:
- Forensic images: `.dd`, `.E01`, `.001`, etc.
- Virtual machines: `.vhd`, `.vhdx`, `.vmdk`
- ISO images: `.iso`
- Linux: `.aff`
- Mobile dumps: `.ufdr`, `.ad1`
- Folders: `./folder`
- Previous IPED exports: `*.iped`

**Examples**:
```bash
java -jar iped.jar -d /path/to/image.dd -o /path/to/output

java -jar iped.jar -d /dev/sda1 -d /dev/sdb1 -o /path/to/output

java -jar iped.jar -d /path/to/folder -o /path/to/output
```

### Optional Arguments

#### `-o, --output PATH`

Output folder for case results. If not specified, defaults to parent directory of first datasource.

```bash
java -jar iped.jar -d /source -o /output/folder
```

#### `-dname NAME` (Multiple allowed)

Display names for datasources. Must match datasources order. Defaults to source filename if not specified.

```bash
java -jar iped.jar -d /source1 -dname "Suspect Phone" -d /source2 -dname "Laptop Drive" -o /output
```

#### `-profile PROFILE`

Processing profile to use. Predefined profiles optimize settings for specific investigation types:

| Profile | Purpose | Use Case |
|---------|---------|----------|
| `forensic` | Complete analysis | Comprehensive investigation |
| `pedo` | Child exploitation | CSAM detection |
| `fastmode` | Faster processing | Quick analysis |
| `blind` | No caching | Court-approved exam |
| `triage` | Rapid assessment | Initial case evaluation |

```bash
java -jar iped.jar -d /source -o /output -profile forensic
```

#### `-l, --keywordlist FILE`

Import keywords from file for automatic searching. Keywords with no matches are filtered out.

```bash
java -jar iped.jar -d /source -o /output -l /path/to/keywords.txt
```

#### `-ocr CATEGORY` (Multiple allowed)

Run OCR only on specific file categories or bookmarks. Can be used multiple times.

```bash
java -jar iped.jar -d /source -o /output -ocr Documents -ocr Images
```

#### `-tz, --timezone TIMEZONE`

Original timezone of FAT filesystems for accurate timestamp interpretation.

Format: `GMT±HH[:MM]`

```bash
java -jar iped.jar -d /source -o /output -tz GMT-3    # São Paulo
java -jar iped.jar -d /source -o /output -tz GMT-5    # Eastern Time
```

#### `-b, --blocksize SIZE`

Sector block size in bytes. Use 4096 for modern 4k sector drives, 512 for standard.

```bash
java -jar iped.jar -d /source -o /output -b 4096
```

#### `-p, --password PASSWORD` (Multiple allowed)

Password(s) for encrypted images/volumes. Can be used multiple times.

```bash
java -jar iped.jar -d /encrypted.vhd -p password123 -o /output

java -jar iped.jar -d /source1 -p pass1 -d /source2 -p pass2 -o /output
```

#### `-log FILE`

Redirect log output to custom file instead of default.

```bash
java -jar iped.jar -d /source -o /output -log /custom/path/logfile.txt
```

#### `-asap FILE`

Include Brazilian Federal Police ASAP case information in HTML report.

```bash
java -jar iped.jar -d /source -o /output -asap /path/to/case.asap
```

#### `-nocontent CATEGORY` (Multiple allowed)

Exclude file contents from report for specific categories/bookmarks (only thumbnails and properties exported).

```bash
java -jar iped.jar -d /source -o /output -nocontent Videos -nocontent LargeFiles
```

#### `-splash MESSAGE`

Custom message to display in splash screen on startup.

```bash
java -jar iped.jar -d /source -o /output -splash "Case Analysis - Badge123"
```

### Boolean Flags

#### `--addowner`

Index file owner information when processing local folders. Slower on network shares.

```bash
java -jar iped.jar -d /network/folder -o /output --addowner
```

#### `--append`

Add datasources to an existing case instead of creating new case.

```bash
java -jar iped.jar -d /new/source -o /existing/case --append
```

#### `--continue`

Continue a stopped or aborted processing run from where it left off.

```bash
java -jar iped.jar -o /case/folder --continue
```

#### `--restart`

Discard last failed processing attempt and start from beginning.

```bash
java -jar iped.jar -o /case/folder --restart
```

#### `--nogui`

Run without GUI progress windows (text mode). Useful for headless systems and servers.

```bash
java -jar iped.jar -d /source -o /output --nogui
```

#### `--nologfile`

Log messages to console (stdout) instead of file.

```bash
java -jar iped.jar -d /source -o /output --nologfile
```

#### `--nopstattachs`

Do not export email attachments from PST/OST files to report.

```bash
java -jar iped.jar -d /source -o /output --nopstattachs
```

#### `--nolinkeditems`

Do not export items linked to chat conversations in report.

```bash
java -jar iped.jar -d /source -o /output --nolinkeditems
```

#### `--portable`

Use relative references to forensic images, enabling case portability across machines.

```bash
java -jar iped.jar -d /images/phone.dd -o /case --portable
```

#### `--downloadInternetData`

Download current data from internet sources to enrich evidence (e.g., WhatsApp media still available on servers).

```bash
java -jar iped.jar -d /source -o /output --downloadInternetData
```

### Advanced Options

#### `-X` (Dynamic Parameters)

Specify module-specific options. Format: `-Xkey=value`

```bash
java -jar iped.jar -d /source -o /output -Xmodule.option=value
```

## 2. IPED Web API Server

**Jar**: `iped-webapi.jar`
**Class**: `iped.engine.webapi.Main`
**Invocation**: `java -jar iped-webapi.jar --host=HOST --port=PORT --sources=URL`

### Purpose

RESTful API server for programmatic access to IPED cases and analysis tools.

### Required Arguments

#### `--sources URL`

URL or endpoint to query for available case sources/repositories.

```bash
java -jar iped-webapi.jar --sources=http://cases.example.com/api/sources

java -jar iped-webapi.jar --sources=file:///data/cases/sources.json
```

### Optional Arguments

#### `--host HOST`

Host/IP address to bind server to.

Default: `0.0.0.0` (all interfaces)

```bash
java -jar iped-webapi.jar --host=0.0.0.0 --port=8080 --sources=http://...

java -jar iped-webapi.jar --host=192.168.1.100 --port=8080 --sources=http://...
```

#### `--port PORT`

Port number for HTTP API server.

Default: `8080`
Range: 1-65535

```bash
java -jar iped-webapi.jar --host=0.0.0.0 --port=9000 --sources=http://...
```

## 3. IPED Search Application

**Jar**: `iped-search-app.jar`
**Class**: `iped.app.ui.AppMain`
**Invocation**: `java -jar iped-search-app.jar [caseFolder]`

### Purpose

GUI application for searching and analyzing IPED cases with advanced investigation tools.

### Optional Arguments

#### Single Case

Specify path to IPED case folder to open on startup.

```bash
java -jar iped-search-app.jar /path/to/case/iped
```

#### Multiple Cases

Open multiple cases for combined search and analysis.

```bash
java -jar iped-search-app.jar --casesFolder=/path/to/cases
```

## Common Usage Patterns

### Basic Processing

```bash
java -jar iped.jar -d /source.dd -o /cases/case001
```

### Forensic Processing with Keywords

```bash
java -jar iped.jar \
  -d /forensic/image.E01 \
  -o /cases/investigation \
  -profile forensic \
  -l /keywords/suspect.txt \
  -tz GMT-5 \
  --nogui
```

### Multi-Source Case

```bash
java -jar iped.jar \
  -d /phone.ufdr -dname "iPhone 12" \
  -d /laptop.dd -dname "Suspect Laptop" \
  -d /network/share -dname "Network Drive" \
  -o /cases/complex_case \
  -profile pedo
```

### Continue Failed Processing

```bash
java -jar iped.jar -o /cases/case001 --continue
```

### Server Deployment

```bash
java -Xmx8g -jar iped-webapi.jar \
  --host=0.0.0.0 \
  --port=8080 \
  --sources=http://evidence-server:9000/sources
```

## Environment Variables

### Memory Configuration

```bash
export IPED_MEMORY=8g
java -jar iped.jar -d /source -o /output
```

### Java Options

```bash
export JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"
java -jar iped.jar -d /source -o /output
```

### Proxy Configuration

```bash
java -Dhttp.proxyHost=proxy.example.com \
  -Dhttp.proxyPort=8080 \
  -jar iped-webapi.jar --sources=http://...
```

## Performance Tuning

### For Large Cases

```bash
java -Xmx16g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar iped.jar -d /massive/image.dd -o /output --nogui
```

### For Server Deployment

```bash
java -Xmx32g \
  -XX:+UseG1GC \
  -XX:+ParallelRefProcEnabled \
  -XX:+AlwaysPreTouch \
  -jar iped-webapi.jar --host=0.0.0.0 --port=8080 --sources=...
```

## Troubleshooting

### Out of Memory

Increase JVM heap size:
```bash
java -Xmx16g -jar iped.jar ...
```

### Slow Network Access

Disable file owner indexing:
```bash
java -jar iped.jar -d /network/source -o /output
```

### Case Corruption Recovery

Use restart flag:
```bash
java -jar iped.jar -o /cases/damaged --restart
```

## Help and Version Information

### Display Help

All applications support:
```bash
java -jar iped.jar --help
java -jar iped.jar -h
java -jar iped.jar /?
```

### Version Information

Check application version by examining JAR manifest or log output.

## References

- Full argument schema: `IPEDProcessingCLI.schema.json`
- UI form schema: `IPEDProcessingCLI.uischema.json`
- Web API schema: `IPEDWebAPICLI.schema.json`
- Implementation: `iped.app.processing.CmdLineArgsImpl`
