import {existsSync, readdirSync, rmSync} from 'node:fs';
import {delimiter, join, resolve} from 'node:path';
import {spawnSync} from 'node:child_process';

const workspaceRoot = resolve(import.meta.dirname, '..');
const specPath = resolve(workspaceRoot, '../specs/84-phase-1-openapi-initial.yaml');
const outputPath = resolve(workspaceRoot, 'src/app/core/api/generated');

function parseJavaMajor(versionText) {
  const match = versionText.match(/version "(\d+)(?:\.(\d+))?/);

  if (!match) {
    return null;
  }

  const first = Number(match[1]);
  const second = match[2] ? Number(match[2]) : null;

  return first === 1 ? second : first;
}

function getJavaCandidates() {
  const candidates = [];
  const envHomes = [process.env.IPED_JAVA_HOME, process.env.JAVA_HOME].filter(Boolean);

  for (const home of envHomes) {
    candidates.push(home);
  }

  if (process.platform === 'win32') {
    const adoptiumDir = 'C:\\Program Files\\Eclipse Adoptium';

    if (existsSync(adoptiumDir)) {
      const jdks = readdirSync(adoptiumDir)
        .filter((entry) => entry.startsWith('jdk-'))
        .sort()
        .reverse()
        .map((entry) => join(adoptiumDir, entry));

      candidates.push(...jdks);
    }
  }

  return [...new Set(candidates)];
}

function findJavaHome() {
  for (const home of getJavaCandidates()) {
    const javaBin = join(home, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');

    if (!existsSync(javaBin)) {
      continue;
    }

    const probe = spawnSync(javaBin, ['-version'], {
      encoding: 'utf8'
    });
    const versionText = `${probe.stdout ?? ''}${probe.stderr ?? ''}`;
    const major = parseJavaMajor(versionText);

    if (probe.status === 0 && major !== null && major >= 11) {
      return { home, javaBin, major };
    }
  }

  return null;
}

const java = findJavaHome();

if (!java) {
  console.error('No compatible JDK (11+) was found for OpenAPI client generation.');
  console.error('Set JAVA_HOME or IPED_JAVA_HOME to a modern JDK and run the command again.');
  process.exit(1);
}

rmSync(outputPath, { recursive: true, force: true });

const cliPath = resolve(
  workspaceRoot,
  'node_modules',
  '.bin',
  process.platform === 'win32' ? 'openapi-generator-cli.cmd' : 'openapi-generator-cli'
);

const child = spawnSync(
  cliPath,
  [
    'generate',
    '-i',
    specPath,
    '-g',
    'typescript-angular',
    '-o',
    outputPath,
    '--additional-properties=providedIn=root,withInterfaces=true,stringEnums=true,useSingleRequestParameter=true,enumPropertyNaming=original,modelPropertyNaming=original'
  ],
  {
    cwd: workspaceRoot,
    stdio: 'inherit',
    env: {
      ...process.env,
      JAVA_HOME: java.home,
      PATH: `${join(java.home, 'bin')}${delimiter}${process.env.PATH ?? ''}`
    },
    shell: process.platform === 'win32'
  }
);

process.exit(child.status ?? 1);
