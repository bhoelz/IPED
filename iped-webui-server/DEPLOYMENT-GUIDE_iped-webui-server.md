# iped-webui-server — Deployment Guide

This guide covers building and running the complete IPED web UI stack as a single deployable unit.

---

## Architecture overview

```
Browser
  │  HTTPS (port 8443 or 443)
  ▼
iped-webui-server  (Spring Boot, port 8080)
  │  /api/**  →  reverse-proxy  →  iped-webapi  (Jersey/Jetty, port 39000)
  │                                    │
  │                                    ▼
  │                               IPED Engine (JVM, Lucene index)
  │
  ├── /workspace          SSR HTML (Rocker templates + HTMX)
  ├── /islands/browser/*  Angular island bundles (hashed JS)
  └── /assets/**          Static CSS, fonts, icons
```

`iped-webui-server` is the **only** externally-exposed process. The engine and `iped-webapi` bind on localhost only.

---

## Prerequisites

| Requirement | Version |
|---|---|
| JDK | 25+ |
| Maven | 3.9+ |
| Node.js (build only) | 22.x (managed by `frontend-maven-plugin`) |
| Docker / Compose | 24+ (optional, for container deployment) |

---

## Building

### Full build (server + Angular islands)

```powershell
# From repo root
mvn -pl iped-webui-server -am package -DskipTests
```

The Angular island bundles are built automatically via `frontend-maven-plugin` during `generate-resources`. To skip the Angular build (e.g. when only Java changed):

```powershell
mvn -pl iped-webui-server package -DskipTests -Dwebui.skipFrontend=true
```

### Output

```
iped-webui-server/target/iped-webui-server-*.jar   ← fat JAR (Spring Boot)
```

---

## Running

### Local / lab (no auth, no TLS)

```powershell
java -jar iped-webui-server/target/iped-webui-server-*.jar `
     --iped.webui.webapi-url=http://localhost:39000
```

Access at `http://localhost:8080`.

### Production (API key + TLS termination at reverse proxy)

```powershell
java -jar iped-webui-server/target/iped-webui-server-*.jar `
     --server.port=8080 `
     --iped.webui.webapi-url=http://127.0.0.1:39000 `
     --iped.webui.admin-password=<secure-password>
```

Place an nginx / Caddy reverse proxy in front for TLS. Example nginx snippet:

```nginx
server {
    listen 443 ssl;
    server_name iped.example.internal;

    ssl_certificate     /etc/ssl/iped.crt;
    ssl_certificate_key /etc/ssl/iped.key;

    location / {
        proxy_pass         http://127.0.0.1:8080;
        proxy_set_header   Host $host;
        proxy_set_header   X-Real-IP $remote_addr;
        proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        # Pass the trace ID through for correlated logging
        proxy_set_header   X-Trace-Id $request_id;
    }
}
```

---

## Configuration reference

All properties can be set via `application.properties`, environment variables, or `--flag` on the command line.

| Property | Env var | Default | Description |
|---|---|---|---|
| `iped.webui.webapi-url` | `IPED_WEBUI_WEBAPI_URL` | `http://localhost:39000` | Base URL of iped-webapi |
| `iped.webui.admin-password` | `IPED_WEBUI_ADMIN_PASSWORD` | *(blank — open mode)* | HTTP Basic password for the `admin` user |
| `server.port` | `SERVER_PORT` | `8080` | HTTP port |
| `logging.level.iped` | `LOGGING_LEVEL_IPED` | `INFO` | Log level for IPED packages |

### iped-webapi (Jersey / engine)

| Property | Default | Description |
|---|---|---|
| `iped.webapi.api-key` | *(blank — open)* | API key required by `ApiKeyAuthFilter` |
| `iped.webapi.audit-log` | *(blank — none)* | Path to JSONL audit log file |
| `--sources` CLI arg | *(required)* | Comma-separated processed-case directories |

---

## Docker / Compose deployment

A minimal `docker-compose.yml` is provided at the repo root (`docker-compose.yml`). It starts:

- **iped-engine** — runs the IPED processing pipeline
- **iped-webapi** — Jersey API on port 39000 (internal)
- **iped-webui-server** — Spring Boot UI on port 8080 (published)
- **kafka** + **zookeeper** — distributed processing bus (optional; skip for single-node)

### Quick start

```powershell
# Build the images (first time or after code changes)
docker compose build

# Start the full stack
docker compose up -d

# Tail logs with trace IDs
docker compose logs -f iped-webui-server iped-webapi
```

### Environment overrides

Copy `.env.example` to `.env` and edit:

```dotenv
IPED_WEBUI_ADMIN_PASSWORD=forensics
IPED_WEBAPI_API_KEY=internal-secret
IPED_CASES_DIR=/data/cases
```

---

## Observability

### Trace IDs

Every request through `iped-webui-server` is assigned an `X-Trace-Id` header (UUID, 32 hex chars). The ID is:
- Written to MDC key `traceId` — visible in all log lines for that request
- Set on the downstream `/api/**` proxy call so Jersey logs carry the same ID
- Returned in the HTTP response for browser console correlation

To include it in log output, add `%X{traceId}` to your `log4j2.xml` pattern:

```xml
<PatternLayout pattern="%d{HH:mm:ss.SSS} %-5p [%X{traceId}] %c{1} — %m%n"/>
```

### Structured logging

Set `logging.structured.format.console=ecs` (Spring Boot 4+) to emit JSON logs compatible with Elastic Common Schema, which includes the `traceId` field natively.

---

## Security notes

- **Content-Security-Policy** is enforced for all workspace pages. The policy allows `'self'` for scripts, `'unsafe-inline'` for styles only (Rocker SSR), `data:` and `blob:` for images, and `frame-src 'self'` for the HTML rendition viewer.
- **CSRF** uses a cookie-based double-submit token compatible with HTMX's `hx-headers` pattern. The `/api/**` proxy path is excluded (Jersey enforces its own API-key auth).
- **Clickjacking**: `X-Frame-Options: DENY` prevents the workspace from being embedded in foreign frames.
- **HSTS**: `Strict-Transport-Security: max-age=31536000; includeSubDomains` — activate only after deploying TLS.
- In **open mode** (no `admin-password` set), all GET requests to the workspace are permitAll. This is intended for single-investigator lab use on a trusted network.

---

## Topology decision — proxy-forever vs consolidation

Current: `iped-webui-server` (Spring Boot) proxies `/api/**` to `iped-webapi` (Jersey on embedded Jetty).

**Decision: keep the two-process model** for 5.0.

Rationale:
- `iped-webapi` runs inside the engine JVM, co-loaded with Lucene and the processing pipeline. Merging it into Spring Boot's classpath would conflict with the engine's class-loading contract.
- The proxy hop adds one HTTP round-trip per API call, which is acceptable for a forensic analyst workflow (human-paced, not high-frequency).
- The two-process split is a clean security boundary: the engine process can bind on localhost only, and the SSR server is the only Internet-facing process.

Future consolidation trigger: if `iped-webapi` is ever decoupled from the engine JVM (e.g. runs as a separate microservice), the proxy can be dropped and the Spring Boot server can call Jersey's REST contract directly.
