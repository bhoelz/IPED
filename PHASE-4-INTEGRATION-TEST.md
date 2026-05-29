# Phase 4 Integration Test Guide

## Overview

This document describes how to test the Phase 4 React Web UI integrated with the JAX-RS Configuration API.

## Prerequisites

- Java 11+ installed and in PATH
- Node.js 16+ and npm installed
- Maven 3.6+ installed
- IPED project built with `mvn clean package`

## Quick Start

### Option 1: Using PowerShell Script (Windows)

```powershell
cd L:\Workspace\IPED
.\start-dev.ps1
```

### Option 2: Using Batch Script (Windows)

```batch
cd L:\Workspace\IPED
start-dev.bat
```

### Option 3: Manual Start

**Terminal 1 - Start ConfigurationServer:**
```bash
cd L:\Workspace\IPED\iped-engine
java -cp target/iped-engine-4.4.0-SNAPSHOT.jar iped.engine.config.api.ConfigurationServer 8080
```

**Terminal 2 - Start React Dev Server:**
```bash
cd L:\Workspace\IPED\iped-ui
npm start
```

## Integration Test Checklist

### API Server Tests (ConfigurationServer)

- [ ] Server starts on port 8080
- [ ] Logs show "Configuration API running on http://localhost:8080/api/v1"
- [ ] Health check: `curl http://localhost:8080/api/v1/schemas`
- [ ] Response contains JSON array of available schemas

### React App Tests

- [ ] App loads at http://localhost:3000
- [ ] Header displays "Configurator" title
- [ ] Sidebar loads with component tree
- [ ] Categories are collapsible (Parsers, Carvers, Viewers, etc.)
- [ ] No console errors in browser DevTools

### API Integration Tests

#### Schema Loading
- [ ] Click on component in sidebar
- [ ] Form fields render based on schema
- [ ] Info panel displays component information
- [ ] No API errors in browser console

#### Configuration Form
- [ ] Text inputs can be edited
- [ ] Number inputs enforce numeric validation
- [ ] Range sliders work with min/max bounds
- [ ] Toggle switches flip on/off
- [ ] Select dropdowns show enum options
- [ ] Textarea fields support multiline input
- [ ] Error messages appear for invalid input

#### Configuration Save
- [ ] Click "Save" button with valid form
- [ ] Toast notification shows success
- [ ] Browser console shows save request
- [ ] API receives POST request to `/api/v1/configurations/{componentName}`

#### Profile Management
- [ ] "Save Profile" button opens prompt
- [ ] Profile name entered is saved to localStorage
- [ ] "Load Profile" button shows saved profiles
- [ ] Profile selection loads configuration from localStorage

#### Tab Navigation
- [ ] "Workspace" tab selected by default
- [ ] "Monitor" and "Logs" tabs are clickable
- [ ] Tab selection is reflected visually

### Error Handling Tests

#### API Unavailable
- [ ] If ConfigurationServer is not running:
  - [ ] App loads but shows no components in sidebar
  - [ ] Selecting a component shows error message
  - [ ] Toast notification shows error
  - [ ] No unhandled exceptions in console

#### Invalid Schema
- [ ] ComponentName that doesn't exist
- [ ] API returns 404 or error response
- [ ] App handles gracefully with error message

#### Validation Errors
- [ ] Enter invalid value in form field
- [ ] Error message appears below field
- [ ] Save button still works but shows validation error toast
- [ ] Invalid field is highlighted in red

### Performance Tests

- [ ] Schema list loads in < 2 seconds
- [ ] Form renders in < 1 second
- [ ] Switching between components is responsive
- [ ] No memory leaks during extended use

## API Endpoint Coverage

The following endpoints should be tested:

- [x] `GET /api/v1/schemas` - List all schemas
- [x] `GET /api/v1/schemas/{componentName}` - Get schema details
- [x] `GET /api/v1/configurations/{componentName}` - Get component config
- [x] `POST /api/v1/schemas/{componentName}/validate` - Validate config
- [x] `POST /api/v1/configurations/{componentName}` - Save config
- [ ] `GET /api/v1/schemas/category/{category}` - Filter by category
- [ ] `POST /api/v1/configurations/diff` - Compare configs
- [ ] `POST /api/v1/configurations/merge` - Merge configs

## Troubleshooting

### ConfigurationServer won't start
- Check if port 8080 is already in use: `netstat -ano | findstr :8080`
- Try different port: `java -cp target/iped-engine-4.4.0-SNAPSHOT.jar iped.engine.config.api.ConfigurationServer 9000`
- Update `.env.development` in iped-ui with new port: `REACT_APP_API_URL=http://localhost:9000/api/v1`

### React app won't start
- Clear node_modules: `rm -r node_modules && npm install`
- Check Node version: `node --version` (should be 16+)
- Check npm version: `npm --version` (should be 7+)

### API connection errors
- Verify ConfigurationServer is running: `curl http://localhost:8080/api/v1/schemas`
- Check browser console for CORS errors
- Verify `.env.development` points to correct API URL

### Form fields don't render
- Check schema file exists in resources/schemas/json/
- Verify schema has valid JSON Schema structure
- Check browser console for parsing errors
- Look at Network tab to see API response

## Expected Results

After successful startup:

1. Terminal 1 (ConfigurationServer):
   ```
   [INFO] Configuration API server started on port 8080
   Configuration API running on http://localhost:8080/api/v1
   Press Ctrl+C to stop
   ```

2. Terminal 2 (React):
   ```
   On Your Network: http://YOUR_IP:3000
   Local: http://localhost:3000
   ```

3. Browser (http://localhost:3000):
   - Header with "Configurator" title and Workspace/Monitor/Logs tabs
   - Sidebar with "System Architect" header and component tree
   - Main panel with form fields for selected component
   - Right panel with component information
   - Save/Load Profile buttons in header

## Next Steps

After successful integration testing:

1. [ ] Create automated E2E tests (Cypress/Playwright)
2. [ ] Add authentication/authorization
3. [ ] Implement request/response logging
4. [ ] Add diff/merge UI visualization
5. [ ] Create production build and deployment guide
6. [ ] Add advanced features (versioning, audit logging, etc.)

## Contact

For issues or questions about Phase 4 integration, refer to the PHASE-3-STATUS.md and API-REFERENCE.md documents.
