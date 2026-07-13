import fs from 'node:fs';
import path from 'node:path';

const distRoot = path.resolve('dist');
const indexHtmlPath = path.join(distRoot, 'index.html');

if (!fs.existsSync(indexHtmlPath)) {
  throw new Error(`Missing build output: ${indexHtmlPath}`);
}

const indexHtml = fs.readFileSync(indexHtmlPath, 'utf8');

if (!indexHtml.includes('<div id="root"></div>')) {
  throw new Error('Built index.html is missing the React root element.');
}

const assetPaths = Array.from(
  indexHtml.matchAll(/(?:src|href)="(\/assets\/[^"]+)"/g),
  (match) => match[1],
);

if (assetPaths.length === 0) {
  throw new Error('Built index.html does not reference any /assets files.');
}

for (const assetPath of assetPaths) {
  const relativeAssetPath = assetPath.replace(/^\//, '').replace(/\//g, path.sep);
  const fullAssetPath = path.join(distRoot, relativeAssetPath.replace(/^assets[\\/]/, `assets${path.sep}`));

  if (!fs.existsSync(fullAssetPath)) {
    throw new Error(`Built index.html references a missing asset: ${assetPath}`);
  }

  if (assetPath.endsWith('.js')) {
    const contents = fs.readFileSync(fullAssetPath, 'utf8');
    if (contents.includes('import.meta.glob')) {
      throw new Error(`Built JavaScript still contains import.meta.glob: ${assetPath}`);
    }
  }
}

console.log(`Verified ${assetPaths.length} built asset reference(s).`);
