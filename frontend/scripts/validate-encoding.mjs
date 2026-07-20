import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, extname } from 'node:path';

const ROOT = process.cwd();
const TEXT_EXTENSIONS = new Set([
  '.ts',
  '.tsx',
  '.js',
  '.jsx',
  '.json',
  '.css',
  '.scss',
  '.html',
  '.md',
  '.yml',
  '.yaml',
  '.java',
  '.mjs',
  '.cjs',
]);
const SKIP_DIRECTORIES = new Set([
  '.git',
  'node_modules',
  'dist',
  'build',
  'coverage',
  'target',
  '.idea',
  '.vite',
  '.next',
]);
const decoder = new TextDecoder('utf-8', { fatal: true });
const invalidFiles = [];

function walk(directory) {
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    if (SKIP_DIRECTORIES.has(entry.name)) {
      continue;
    }
    const fullPath = join(directory, entry.name);
    if (entry.isDirectory()) {
      walk(fullPath);
      continue;
    }
    if (!entry.isFile() || !TEXT_EXTENSIONS.has(extname(entry.name).toLowerCase())) {
      continue;
    }
    if (statSync(fullPath).size === 0) {
      continue;
    }
    try {
      decoder.decode(readFileSync(fullPath));
    } catch {
      invalidFiles.push(fullPath.replace(`${ROOT}\\`, '').replaceAll('\\', '/'));
    }
  }
}

walk(ROOT);

if (invalidFiles.length > 0) {
  console.error('Invalid UTF-8 source files detected:');
  for (const file of invalidFiles) {
    console.error(`- ${file}`);
  }
  process.exit(1);
}

console.log('UTF-8 validation passed.');
