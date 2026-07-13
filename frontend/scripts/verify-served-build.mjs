const defaultBaseUrl = 'http://127.0.0.1:4173';
const baseUrl = (process.argv[2] ?? process.env.EDURITE_FRONTEND_BASE_URL ?? defaultBaseUrl).replace(/\/$/, '');
const criticalRoutes = ['/', '/student/universities'];

const assert = (condition, message) => {
  if (!condition) {
    throw new Error(message);
  }
};

const fetchText = async (url) => {
  const response = await fetch(url);
  const body = await response.text();
  return { response, body };
};

const fetchWithRetry = async (url, attempts = 20, delayMs = 500) => {
  let lastError;
  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    try {
      return await fetchText(url);
    } catch (error) {
      lastError = error;
      await new Promise((resolve) => setTimeout(resolve, delayMs));
    }
  }
  throw lastError;
};

const { response: rootResponse, body: rootHtml } = await fetchWithRetry(`${baseUrl}/`);
assert(rootResponse.ok, `Root request failed with status ${rootResponse.status}`);
assert((rootResponse.headers.get('content-type') ?? '').includes('text/html'), 'Root response is not HTML.');
assert(rootHtml.includes('<div id="root"></div>'), 'Root HTML is missing the React root element.');

const assetPaths = Array.from(
  rootHtml.matchAll(/(?:src|href)="(\/assets\/[^"]+)"/g),
  (match) => match[1],
);
assert(assetPaths.length >= 2, 'Root HTML did not expose the expected JS and CSS assets.');

for (const route of criticalRoutes) {
  const { response, body } = await fetchText(`${baseUrl}${route}`);
  assert(response.ok, `Critical route ${route} failed with status ${response.status}`);
  assert((response.headers.get('content-type') ?? '').includes('text/html'), `Critical route ${route} is not served as HTML.`);
  assert(body.includes('<div id="root"></div>'), `Critical route ${route} did not return the SPA shell.`);
}

for (const assetPath of assetPaths) {
  const { response, body } = await fetchText(`${baseUrl}${assetPath}`);
  assert(response.ok, `Asset ${assetPath} failed with status ${response.status}`);

  if (assetPath.endsWith('.js')) {
    assert((response.headers.get('content-type') ?? '').includes('javascript'), `JavaScript asset ${assetPath} has the wrong content type.`);
    assert(!body.includes('import.meta.glob'), `JavaScript asset ${assetPath} still contains import.meta.glob.`);
  }

  if (assetPath.endsWith('.css')) {
    assert((response.headers.get('content-type') ?? '').includes('text/css'), `CSS asset ${assetPath} has the wrong content type.`);
  }
}

console.log(`Verified served frontend at ${baseUrl} across ${criticalRoutes.length} route(s) and ${assetPaths.length} asset(s).`);
