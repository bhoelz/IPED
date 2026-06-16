# Companion App Handoff Protocol (iped-app Phase 3)

## Purpose

The IPED browser UI is the primary analyst interface for 5.0, but several content types
require native desktop capabilities (LibreOffice embedding, CAD SDKs, OS shell viewers).
Rather than run a full Swing UI alongside the browser, a slim **companion app** handles
only these native-only viewers.

The handoff protocol defines how the browser UI asks the companion app to open an item.

## Protocol: Local HTTP + JWT

The companion app runs a loopback HTTP server on a configurable port (default `18743`).
The browser UI fetches the item content from `iped-webapi` and instructs the companion
to display it via a `POST /open` request.

### Companion server endpoint

```
POST http://127.0.0.1:18743/open
Authorization: Bearer <session-token>
Content-Type: application/json

{
  "sourceId": "case-001",
  "docId":    42,
  "mimeType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "itemName": "contract.docx",
  "contentUrl": "http://localhost:8080/v2/sources/case-001/items/42/content",
  "apiKey":    "<webapi-api-key>"
}
```

Response `200 OK` → companion opened the viewer.
Response `415 Unsupported Media Type` → no native viewer for this MIME type.
Response `503 Service Unavailable` → companion app not running.

### Session token

The browser UI holds a short-lived HMAC-SHA256 token scoped to the local loopback
interface. The companion verifies the token before processing any open request.
Token lifetime: 1 hour; rotated on companion restart.

The token is issued by the companion at startup and printed to stdout in the format:

```
IPED_COMPANION_TOKEN=<hex-token>
```

The iped-webui-server reads this token (via an env var or a companion-status endpoint)
and injects it into the workspace page so the browser JS can include it.

## Java interface

```java
// iped.app.companion.CompanionHandoff
public interface CompanionHandoff {
    /** Returns true if the companion app is reachable on the loopback port. */
    boolean isAvailable();

    /** Asks the companion to open the given item. Returns false if unavailable or unsupported. */
    boolean openItem(CompanionHandoffRequest request) throws IOException;
}
```

```java
// iped.app.companion.CompanionHandoffRequest
public record CompanionHandoffRequest(
    String sourceId,
    int    docId,
    String mimeType,
    String itemName,
    String contentUrl,
    String apiKey
) {}
```

## Browser side

The browser workspace page includes a "Open in Companion" button in the viewer toolbar
(visible only when `iped-companion-available` attribute is `true` on the `<iped-viewer>`
island). Clicking it sends:

```javascript
fetch('http://127.0.0.1:18743/open', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + IPED_COMPANION_TOKEN,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ sourceId, docId, mimeType, itemName, contentUrl, apiKey })
})
```

## Supported MIME types (initial scope)

| MIME type | Native viewer |
|---|---|
| `application/vnd.openxmlformats-officedocument.*` | LibreOffice |
| `application/msword`, `application/vnd.ms-excel`, `application/vnd.ms-powerpoint` | LibreOffice |
| `application/vnd.oasis.opendocument.*` | LibreOffice |
| `application/dxf`, `image/vnd.dxf` | CADViewer |
| `*/*` (fallback) | ReferencedFileViewer (OS shell `xdg-open` / `open` / `ShellExecute`) |

## Security constraints

1. Companion listens **only** on `127.0.0.1` (loopback) — never on `0.0.0.0`.
2. Token is validated on every request; missing or invalid token → `401`.
3. The companion never accepts `contentUrl` pointing to hosts other than `localhost` or
   the configured `iped-webapi` host.
4. All opened items are logged to the audit log (same JSONL format as `McpAuditLog`).

## Versioning

The companion exposes `GET /version` → `{"version":"4.4.0","protocol":1}`.
The browser UI checks the protocol version before sending an open request; version
mismatch shows a "Please update your companion app" toast.
