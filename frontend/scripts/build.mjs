import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const postcss = require('postcss');
const tailwindcss = require('tailwindcss');
const autoprefixer = require('autoprefixer');
const esbuild = require('esbuild');

const root = process.cwd();
const workspaceRoot = path.resolve(root, '..');
const distRoot = path.resolve(root, 'dist');
const assetsRoot = path.join(distRoot, 'assets');
const tempSrcRoot = path.resolve(root, 'build-temp-src');
const entryHtml = path.resolve(root, 'index.html');
const entryStyles = path.resolve(root, 'src', 'styles', 'index.css');
const outputCssPath = '/assets/index.css';
const outputScriptPath = '/assets/main.js';
const outputMainJsPath = path.join(distRoot, 'assets', 'main.js');
const parseEnvFile = (filePath) => {
  if (!fs.existsSync(filePath)) {
    return {};
  }
  const values = {};
  const lines = fs.readFileSync(filePath, 'utf8').split(/\r?\n/);
  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#')) {
      continue;
    }
    const separator = line.indexOf('=');
    if (separator < 0) {
      continue;
    }
    const key = line.slice(0, separator).trim();
    values[key] = line.slice(separator + 1).trim().replace(/^['"]|['"]$/g, '');
  }
  return values;
};

const loadBuildEnv = () => {
  const files = [
    path.join(workspaceRoot, '.env'),
    path.join(workspaceRoot, '.env.production'),
    path.join(workspaceRoot, '.env.local'),
    path.join(workspaceRoot, '.env.production.local'),
    path.join(root, '.env'),
    path.join(root, '.env.production'),
    path.join(root, '.env.local'),
    path.join(root, '.env.production.local'),
  ];
  return files.reduce((merged, filePath) => Object.assign(merged, parseEnvFile(filePath)), {});
};

const buildEnv = {
  ...loadBuildEnv(),
  ...Object.fromEntries(
    Object.entries(process.env).filter(([key]) => key.startsWith('VITE_')),
  ),
};
const importMetaEnv = {
  MODE: 'production',
  DEV: false,
  PROD: true,
  BASE_URL: '/',
};

for (const [key, value] of Object.entries(buildEnv)) {
  if (key.startsWith('VITE_')) {
    importMetaEnv[key] = value;
  }
}

const writeIndexHtml = () => {
  const source = fs.readFileSync(entryHtml, 'utf8');
  const next = source
    .replace(
      /<script type="module" src="\/src\/main\.tsx"><\/script>/,
      `<link rel="stylesheet" href="${outputCssPath}" />\n    <script type="module" src="${outputScriptPath}"></script>`,
    );
  fs.writeFileSync(path.join(distRoot, 'index.html'), next, 'utf8');
};

const rewriteAliasImports = (source, filePath) => {
  const fileDirectory = path.dirname(filePath);
  return source.replace(/(['"])@\/([^'"`]+)\1/g, (_match, quote, target) => {
    const resolvedTarget = path.resolve(tempSrcRoot, target);
    let relativeTarget = path.relative(fileDirectory, resolvedTarget).replace(/\\/g, '/');
    if (!relativeTarget.startsWith('.')) {
      relativeTarget = `./${relativeTarget}`;
    }
    return `${quote}${relativeTarget}${quote}`;
  });
};

const stageSources = (sourceDir, outputDir) => {
  fs.mkdirSync(outputDir, { recursive: true });
  for (const entry of fs.readdirSync(sourceDir, { withFileTypes: true })) {
    const sourcePath = path.join(sourceDir, entry.name);
    const outputPath = path.join(outputDir, entry.name);
    if (entry.isDirectory()) {
      stageSources(sourcePath, outputPath);
      continue;
    }
    const content = fs.readFileSync(sourcePath);
    if (entry.name.endsWith('.ts') || entry.name.endsWith('.tsx')) {
      const rewritten = rewriteAliasImports(content.toString('utf8'), outputPath);
      fs.writeFileSync(outputPath, rewritten, 'utf8');
      continue;
    }
    fs.writeFileSync(outputPath, content);
  }
};

const buildCss = async () => {
  const source = fs.readFileSync(entryStyles, 'utf8');
  const result = await postcss([
    tailwindcss({
      content: [entryHtml, path.join(root, 'src', '**/*.{ts,tsx}')],
      theme: {
        extend: {
          colors: {
            primary: {
              50: '#eff6ff',
              100: '#dbeafe',
              200: '#bfdbfe',
              300: '#93c5fd',
              400: '#60a5fa',
              500: '#2563eb',
              600: '#1d4ed8',
              700: '#1e40af',
              800: '#1e3a8a',
              900: '#172554',
            },
          },
          boxShadow: {
            card: '0 8px 25px rgba(15, 23, 42, 0.08)',
          },
        },
      },
      plugins: [],
    }),
    autoprefixer(),
  ]).process(source, {
    from: entryStyles,
    to: path.join(assetsRoot, 'index.css'),
  });
  fs.writeFileSync(path.join(assetsRoot, 'index.css'), result.css, 'utf8');
};

const runEsbuild = () => {
  const envLiteral = JSON.stringify(importMetaEnv);
  esbuild.buildSync({
    absWorkingDir: root,
    entryPoints: [path.join(tempSrcRoot, 'main.tsx')],
    bundle: true,
    platform: 'browser',
    format: 'esm',
    target: 'es2020',
    jsx: 'automatic',
    tsconfigRaw: {
      compilerOptions: {
        jsx: 'react-jsx',
        module: 'esnext',
        target: 'es2020',
      },
    },
    outfile: outputMainJsPath,
    publicPath: '/assets',
    assetNames: '[name]-[hash]',
    loader: {
      '.png': 'file',
      '.jpg': 'file',
      '.jpeg': 'file',
      '.svg': 'file',
      '.gif': 'file',
      '.webp': 'file',
      '.css': 'empty',
    },
    define: {
      'import.meta.env': envLiteral,
    },
  });
};

const rewriteEmittedAssetUrls = () => {
  const bundle = fs.readFileSync(outputMainJsPath, 'utf8');
  const nextBundle = bundle.replace(
    /(["'])\.\/([^"'`]+\.(?:png|jpe?g|svg|gif|webp))\1/g,
    (_match, quote, assetPath) => `${quote}/assets/${assetPath}${quote}`,
  );
  fs.writeFileSync(outputMainJsPath, nextBundle, 'utf8');
};

fs.rmSync(distRoot, { recursive: true, force: true });
fs.rmSync(tempSrcRoot, { recursive: true, force: true });
fs.mkdirSync(assetsRoot, { recursive: true });
stageSources(path.resolve(root, 'src'), tempSrcRoot);

try {
  await buildCss();
  runEsbuild();
  rewriteEmittedAssetUrls();
  writeIndexHtml();
} finally {
  fs.rmSync(tempSrcRoot, { recursive: true, force: true });
}
