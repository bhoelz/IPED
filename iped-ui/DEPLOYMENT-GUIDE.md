# iped-ui Deployment Guide

> Note: this guide originally referenced `start-dev.ps1`/`start-dev.bat`/`start-api.ps1`
> helper scripts and companion `PHASE-4-*.md` docs at the repo root — none of those exist
> in the current tree, so the commands below use direct `mvn`/`java`/`npm` invocations
> instead. `ConfigurationServer` also moved from `iped-engine` to `iped-engine-core` during
> the engine module split; paths below reflect that.

## Build Instructions

### API-Only JAR (Recommended)
Builds a minimal JAR with only configuration API dependencies:

```bash
cd iped-engine-parent/iped-engine-core
mvn clean package -P api-only -DskipTests
```

**Features**:
- Excludes UI/viewer modules that require JavaFX
- Smaller artifact, faster startup
- Standalone REST API server

### Full JAR
Builds the complete IPED application (requires JavaFX):

```bash
mvn clean package -DskipTests
```

## Running the Servers

### API Server

```bash
java -cp iped-engine-parent/iped-engine-core/target/<jar-name>.jar \
     iped.engine.config.api.ConfigurationServer 8080
```

(Check the actual built JAR name under `target/` — it varies by version.)

### React Dev Server

```bash
cd iped-ui
npm install
npm start
```

(Assumes the API server above is running separately on `localhost:8080`.)

## Configuration

### API Base URL
Edit `iped-ui/.env.development`:
```
REACT_APP_API_URL=http://localhost:8080/api/v1
```

### API Port
Change the port in the server command, then update `iped-ui/.env.development` to match:
```bash
java -cp iped-engine-parent/iped-engine-core/target/<jar-name>.jar \
     iped.engine.config.api.ConfigurationServer 9000
```

## Production Deployment

### Prerequisites
- Java 25
- Node.js 16+ for the React build

### Build React Application for Production

```bash
cd iped-ui
npm run build
```

**Output**: `build/` directory with static files. Serve with any static file server.

### Docker Deployment

```dockerfile
FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY iped-engine-parent/iped-engine-core/target/<jar-name>.jar app.jar
COPY iped-ui/build /var/www/html

EXPOSE 8080

CMD ["java", "-cp", "app.jar", "iped.engine.config.api.ConfigurationServer", "8080"]
```

## Troubleshooting

### API Returns 500 Error
**Cause**: Configuration resources not loading
**Solution**:
1. Verify the JAR contains resource files
2. Check classpath is correct
3. Ensure `ConfigurationManager` is initialized

### React can't connect to API
**Cause**: Wrong API URL or API not running
**Solution**:
1. Verify API is running: `curl http://localhost:8080/api/v1/schemas`
2. Check `iped-ui/.env.development` has the correct URL
3. Check browser DevTools Network tab for failed requests

### Port Already in Use
1. Find process: `netstat -ano | findstr :8080` (Windows) or `lsof -i :8080` (Unix)
2. Kill it, or use a different port and update the config

### "JavaFX not found" Error
**Solution**: Use the API-only JAR (`-P api-only`), or run on Java 25 (includes fallback support).

## Verification Checklist

- [ ] `ConfigurationServer` running on `http://localhost:8080`
- [ ] React app loaded at `http://localhost:3000`
- [ ] API test: `curl http://localhost:8080/api/v1/schemas`
- [ ] Sidebar shows component list; selecting one loads a configuration form
- [ ] Save button sends a POST request; toast notification appears
- [ ] No console errors in browser DevTools

## Project Structure

```
iped-engine-parent/iped-engine-core/
  src/main/java/iped/engine/config/
    api/
      ConfigurationServer.java
      ConfigurationServerLauncher.java
      SchemaResource.java
      ConfigurationResource.java
      SchemaAPIController.java
      ConfigurationAPIController.java
    schema/
      ConfigurationDiffMerge.java
  pom.xml (with api-only profile)

iped-ui/
  public/index.html
  src/
    components/{ConfigurationPanel,SchemaSelector,InfoPanel,Toast}.jsx
    api/configAPI.js
    App.jsx, App.css, index.js
  .env.development (API_URL config)
  package.json
```

## Status

This guide describes a working API-only JAR + React config UI setup. iped-ui's longer-term
fate (keep as a standalone React app vs. fold into the SSR/HTMX/Angular-islands web UI) is
still an open decision — see [iped-ui-ROADMAP.md](../docs/roadmaps/iped-ui-ROADMAP.md),
`ISSUE-335`.
