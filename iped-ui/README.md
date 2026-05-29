# IPED Configuration Web UI

This is a React-based web UI for managing IPED configuration. It connects to the JAX-RS Configuration API running on an embedded Jetty server.

## Prerequisites

- Node.js 16+ and npm
- IPED ConfigurationServer running on http://localhost:8080

## Setup

Install dependencies:

```bash
npm install
```

## Development

Start the development server:

```bash
npm start
```

The app will open at http://localhost:3000 and connect to the API at http://localhost:8080/api/v1.

## Production Build

Create an optimized production build:

```bash
npm run build
```

This creates a `build/` directory with static files ready for deployment.

## API Integration

The UI communicates with the ConfigurationServer JAX-RS API. Configure the API URL in `.env.development` or `.env.production`.

### Available API Endpoints

- `GET /api/v1/schemas` - List all available configuration schemas
- `GET /api/v1/schemas/{componentName}` - Get JSON schema for a component
- `GET /api/v1/configurations` - Get all current configurations
- `GET /api/v1/configurations/{componentName}` - Get specific component configuration
- `POST /api/v1/schemas/{componentName}/validate` - Validate configuration
- `POST /api/v1/configurations/{componentName}` - Save configuration
- `POST /api/v1/configurations/diff` - Compare two configurations
- `POST /api/v1/configurations/merge` - Merge three configurations

## Project Structure

```
src/
├── components/
│   ├── ConfigurationPanel.jsx    # Main form rendering component
│   ├── SchemaSelector.jsx        # Sidebar with component tree
│   ├── InfoPanel.jsx             # Right panel with component info
│   └── Toast.jsx                 # Notification component
├── api/
│   └── configAPI.js              # API client wrapper
├── App.jsx                       # Main application component
├── App.css                       # Application styling
└── index.js                      # Entry point
```

## Features

- Browse and select configuration components by category
- View and edit configuration properties with validation
- Real-time form validation with error messages
- Save and load configuration profiles via localStorage
- Notifications for save success/errors
- Responsive layout with sidebar, main panel, and info panel

## Browser Support

Works with modern browsers (Chrome, Firefox, Safari, Edge).
