/**
 * Post-build script: reads the Angular output stats and writes
 * dist/islands/manifest.json consumed by the Spring SSR server.
 *
 * manifest.json shape:
 * {
 *   "version": "1",
 *   "builtAt": "<ISO timestamp>",
 *   "entrypoint": "/islands/browser/main.HASH.js",
 *   "chunks": {
 *     "results-grid": "/islands/browser/chunk-HASH.js",
 *     "gallery":      "/islands/browser/chunk-HASH.js",
 *     ...
 *   }
 * }
 *
 * The "chunks" map is best-effort: it matches Angular's lazy chunk filenames
 * to island names via the component file they originate from.
 * The SSR server MAY preload individual island chunks as <link rel="modulepreload">
 * when only a subset of islands is needed on the page.
 */

import {readFileSync, readdirSync, writeFileSync} from 'node:fs';
import {join, basename} from 'node:path';
import {fileURLToPath} from 'node:url';

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const distDir   = join(__dirname, '..', 'dist', 'islands', 'browser');
const outFile   = join(__dirname, '..', 'dist', 'islands', 'manifest.json');

// Island name → source path fragment (matches what esbuild uses in chunk naming)
const ISLAND_PATTERNS = {
  'results-grid': 'results-grid',
  'gallery':      'gallery',
  'hex-viewer':   'hex-viewer',
  'timeline':     'timeline',
  'graph':        'graph',
};

let files;
try {
  files = readdirSync(distDir);
} catch (e) {
  console.error(`[islands-manifest] dist/islands/browser not found — run "npm run build:islands" first.`);
  process.exit(1);
}

const jsFiles = files.filter(f => f.endsWith('.js'));

// Find the main entrypoint (no chunk- prefix)
const mainFile = jsFiles.find(f => /^main[.\-]/.test(f));
if (!mainFile) {
  console.error(`[islands-manifest] Could not find main.*.js in ${distDir}`);
  process.exit(1);
}

// Match chunk files to islands. Angular's esbuild output names lazy chunks
// based on the module path, typically "chunk-<HASH>.js" or "<island-name>-<HASH>.js".
// We read the chunk content to find which island component it contains.
const chunkFiles = jsFiles.filter(f => f !== mainFile);

const chunks = {};
for (const [islandName, pattern] of Object.entries(ISLAND_PATTERNS)) {
  // Try direct name match first
  const direct = chunkFiles.find(f => f.startsWith(pattern + '.') || f.includes(pattern + '-'));
  if (direct) {
    chunks[islandName] = `/islands/browser/${direct}`;
    continue;
  }
  // Fall back to content scan (the chunk will contain the component selector string)
  const selector = `iped-${islandName}-impl`;
  const byContent = chunkFiles.find(f => {
    try {
      return readFileSync(join(distDir, f), 'utf8').includes(selector);
    } catch {
      return false;
    }
  });
  if (byContent) {
    chunks[islandName] = `/islands/browser/${byContent}`;
  }
}

const manifest = {
  version:    '1',
  builtAt:    new Date().toISOString(),
  entrypoint: `/islands/browser/${mainFile}`,
  chunks,
};

writeFileSync(outFile, JSON.stringify(manifest, null, 2));
console.log(`[islands-manifest] Written to ${outFile}`);
console.log(`  entrypoint: ${manifest.entrypoint}`);
for (const [name, path] of Object.entries(chunks)) {
  console.log(`  ${name.padEnd(14)}: ${path}`);
}
const missing = Object.keys(ISLAND_PATTERNS).filter(k => !chunks[k]);
if (missing.length) {
  console.warn(`[islands-manifest] WARNING: no chunk found for: ${missing.join(', ')}`);
}
