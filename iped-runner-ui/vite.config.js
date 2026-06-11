import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'

/**
 * Produces a single self-contained IIFE bundle that mounts the React app.
 * Output: ../iped-runner/src/main/resources/static/runner/dist/runner-island.js
 *
 * To run a Vite dev server (with HMR) proxying the Spring backend:
 *   npm run dev
 * Then access the app at http://localhost:5173 while Spring runs at :8092.
 */
export default defineConfig({
  plugins: [react()],

  build: {
    lib: {
      entry: resolve(__dirname, 'src/main.jsx'),
      formats: ['iife'],
      name: 'IpedRunner',
      fileName: () => 'runner-island.js',
    },
    outDir: resolve(__dirname, '../iped-runner/src/main/resources/static/runner/dist'),
    emptyOutDir: true,
    // Include source maps only in CI/debug builds
    sourcemap: false,
  },

  // Dev server: proxy Spring REST endpoints so the Vite app can call /run and /browse
  server: {
    port: 5173,
    proxy: {
      '/run':    'http://localhost:8092',
      '/browse': 'http://localhost:8092',
      '/runner': 'http://localhost:8092', // static assets (runner.css, fonts, etc.)
    },
  },
})
