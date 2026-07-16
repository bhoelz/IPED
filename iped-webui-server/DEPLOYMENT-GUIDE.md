# iped-webui-server — Deployment Guide

This is the canonical deployment guide for the Spring Boot web UI server.

## Build

From the repository root:

```powershell
mvn -pl iped-webui-server -am package -DskipTests
```

The packaged Spring Boot executable is written to
`iped-webui-server/target/iped-webui-server-*.jar`. Use
`-Dwebui.skipFrontend=true` when only server-side code is being rebuilt.

## Configuration and run

```powershell
java -jar iped-webui-server/target/iped-webui-server-*.jar `
  --iped.webui.webapi-url=http://127.0.0.1:39000 `
  --iped.webui.admin-password=<secret>
```

Important settings are `iped.webui.webapi-url`, `iped.webui.admin-password`,
`server.port`, and `logging.level.iped`. They may be supplied as properties,
environment variables, or command-line flags. Keep `iped-webapi` bound to
localhost or a private network.

## TLS termination

Expose only the web UI server through nginx or another reverse proxy. Terminate
TLS at the proxy and forward the original host, client address, protocol, and
trace identifier:

```nginx
location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Trace-Id $request_id;
}
```

Use HSTS only after HTTPS is active and configured correctly.

## Docker / Compose

```powershell
docker compose build
docker compose up -d
docker compose logs -f iped-webui-server iped-webapi
```

Set `IPED_WEBUI_ADMIN_PASSWORD`, `IPED_WEBAPI_API_KEY`, and the case directory
through the deployment environment. Do not publish the internal API port.

## Observability

Requests carry an `X-Trace-Id` value through the proxy, Jersey API, and engine
logs. Include `%X{traceId}` in the Log4j2 pattern or enable structured ECS
logging. Monitor startup failures, authentication failures, proxy errors, and
latency for `/workspace` and `/api/**`.

## Security notes

- Keep authentication enabled outside a trusted single-investigator lab.
- Enforce CSRF for browser mutations and API-key authentication on the internal API.
- Preserve CSP, clickjacking protection, and secure cookie settings.
- Store passwords, API keys, and TLS private keys outside source control.
- Restrict filesystem access to the case directories and application logs.

For the detailed topology and configuration reference, see
[`DEPLOYMENT-GUIDE_iped-webui-server.md`](DEPLOYMENT-GUIDE_iped-webui-server.md).
