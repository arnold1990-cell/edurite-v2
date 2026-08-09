import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const testEmailPrefix = __ENV.TEST_EMAIL_PREFIX || 'perf-user';
const testPassword = __ENV.TEST_PASSWORD || 'PerfTest@12345';
const perfTestUserCount = Number(__ENV.PERF_TEST_USER_COUNT || '100');
const tokenRefreshSkewMs = Number(__ENV.TOKEN_REFRESH_SKEW_SECONDS || '60') * 1000;

const loginDuration = new Trend('endpoint_login_duration', true);
const readinessDuration = new Trend('endpoint_readiness_duration', true);
const careersDuration = new Trend('endpoint_careers_duration', true);
const coursesDuration = new Trend('endpoint_courses_duration', true);
const bursariesDuration = new Trend('endpoint_bursaries_duration', true);
const accountMeDuration = new Trend('endpoint_account_me_duration', true);
const notificationsDuration = new Trend('endpoint_notifications_unread_count_duration', true);
const recommendationsDuration = new Trend('endpoint_recommendations_duration', true);
const authenticatedApiDuration = new Trend('authenticated_api_duration', true);

let accessToken = null;
let refreshToken = null;
let accessTokenExpiresAt = 0;

const stagePresets = {
  '100': { vus: 100, duration: '5m' },
  '500': { vus: 500, duration: '10m' },
  '1000': { vus: 1000, duration: '15m' },
  '5000': { vus: 5000, duration: '20m' },
  '10000': { vus: 10000, duration: '30m' },
  '25000': { vus: 25000, duration: '45m' },
  '50000': { vus: 50000, duration: '60m' }
};

const selectedStage = __ENV.STAGE;
const selectedPreset = selectedStage ? stagePresets[selectedStage] : null;

if (selectedStage && !selectedPreset) {
  throw new Error(`Unsupported STAGE=${selectedStage}. Use one of: ${Object.keys(stagePresets).join(', ')}`);
}

const stageVus = selectedPreset ? selectedPreset.vus : Number(__ENV.VUS || '100');
const stageDuration = __ENV.DURATION || (selectedPreset ? selectedPreset.duration : '5m');

if (!Number.isInteger(perfTestUserCount) || perfTestUserCount < 1 || perfTestUserCount > 50000) {
  throw new Error(`PERF_TEST_USER_COUNT must be an integer from 1 to 50000. Received: ${__ENV.PERF_TEST_USER_COUNT}`);
}

if (!Number.isInteger(stageVus) || stageVus < 1) {
  throw new Error(`VUS/STAGE must resolve to a positive integer. Received VUS=${__ENV.VUS || ''} STAGE=${selectedStage || ''}`);
}

if (stageVus > perfTestUserCount) {
  throw new Error(`Configuration error: requested ${stageVus} VUs but PERF_TEST_USER_COUNT=${perfTestUserCount}. Seed enough perf users first.`);
}

export const options = {
  scenarios: {
    realistic_api_journey: {
      executor: 'constant-vus',
      vus: stageVus,
      duration: stageDuration,
      tags: { scenario: selectedStage ? `stage_${selectedStage}` : 'local_validation' }
    }
  },
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.05', abortOnFail: true, delayAbortEval: '30s' }],
    http_req_duration: [{ threshold: 'p(95)<5000', abortOnFail: true, delayAbortEval: '30s' }],
    checks: ['rate>0.95'],
    'checks{check:readiness 200}': [{ threshold: 'rate>0.99', abortOnFail: true, delayAbortEval: '15s' }]
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max']
};

function login() {
  const userId = exec.vu.idInTest;
  const email = `${testEmailPrefix}-${userId}@example.test`;
  const payload = JSON.stringify({ email, password: testPassword });
  const res = http.post(`${baseUrl}/api/auth/login`, payload, {
    headers: { 'Content-Type': 'application/json', 'X-Request-Id': `k6-login-${userId}` },
    tags: { name: 'POST /api/auth/login', endpoint: 'auth_login', auth: 'none' }
  });
  loginDuration.add(res.timings.duration);

  const loginOk = check(res, {
    'login success': r => r.status === 200
  });

  if (!loginOk) {
    return null;
  }

  try {
    const token = res.json('accessToken');
    const responseRefreshToken = res.json('refreshToken');
    const expiresInSeconds = Number(res.json('accessTokenExpiresIn') || 3600);
    check(token, {
      'token exists': value => typeof value === 'string' && value.length > 0
    });
    accessToken = token;
    refreshToken = typeof responseRefreshToken === 'string' && responseRefreshToken.length > 0 ? responseRefreshToken : null;
    accessTokenExpiresAt = Date.now() + expiresInSeconds * 1000;
    return accessToken;
  } catch (_) {
    check(null, {
      'token exists': value => typeof value === 'string' && value.length > 0
    });
    return null;
  }
}

function refreshAccessToken() {
  if (!refreshToken) {
    return login();
  }

  const res = http.post(`${baseUrl}/api/auth/refresh`, JSON.stringify({ refreshToken }), {
    headers: { 'Content-Type': 'application/json', 'X-Request-Id': `k6-refresh-${exec.vu.idInTest}` },
    tags: { name: 'POST /api/auth/refresh', endpoint: 'auth_refresh', auth: 'none' }
  });

  const refreshOk = check(res, {
    'token refresh success': r => r.status === 200
  });

  if (!refreshOk) {
    accessToken = null;
    refreshToken = null;
    accessTokenExpiresAt = 0;
    return login();
  }

  try {
    accessToken = res.json('accessToken');
    refreshToken = res.json('refreshToken') || refreshToken;
    accessTokenExpiresAt = Date.now() + Number(res.json('accessTokenExpiresIn') || 3600) * 1000;
    check(accessToken, {
      'token exists': value => typeof value === 'string' && value.length > 0
    });
    return accessToken;
  } catch (_) {
    accessToken = null;
    refreshToken = null;
    accessTokenExpiresAt = 0;
    return login();
  }
}

function ensureAccessToken() {
  if (!accessToken) {
    return login();
  }
  if (Date.now() + tokenRefreshSkewMs >= accessTokenExpiresAt) {
    return refreshAccessToken();
  }
  return accessToken;
}

function addAuthenticatedDuration(res, trend) {
  trend.add(res.timings.duration);
  authenticatedApiDuration.add(res.timings.duration);
}

function getReadiness() {
  const res = http.get(`${baseUrl}/actuator/health/readiness`, {
    tags: { name: 'GET readiness', endpoint: 'readiness', auth: 'none' }
  });
  readinessDuration.add(res.timings.duration);
  check(res, { 'readiness 200': r => r.status === 200 });
  return res;
}

function getAuthenticated(path, name, endpoint, trend, headers, expectedCheckName) {
  const res = http.get(`${baseUrl}${path}`, {
    headers,
    tags: { name, endpoint, auth: 'bearer' }
  });
  addAuthenticatedDuration(res, trend);
  check(res, { [expectedCheckName]: r => r.status >= 200 && r.status < 300 });
  return res;
}

export default function () {
  const token = ensureAccessToken();
  if (!token) {
    sleep(1);
    return;
  }

  const authHeaders = {
    Authorization: `Bearer ${token}`,
    'X-Request-Id': `k6-${exec.vu.idInTest}-${exec.scenario.iterationInTest}`
  };

  getReadiness();
  getAuthenticated('/api/careers?page=0&size=20', 'GET /api/careers', 'careers', careersDuration, authHeaders, 'careers success');
  getAuthenticated('/api/courses?page=0&size=20', 'GET /api/courses', 'courses', coursesDuration, authHeaders, 'courses success');
  getAuthenticated('/api/bursaries?page=0&size=20', 'GET /api/bursaries', 'bursaries', bursariesDuration, authHeaders, 'bursaries success');
  getAuthenticated('/api/account/me', 'GET /api/account/me', 'account_me', accountMeDuration, authHeaders, 'account/me success');
  getAuthenticated('/api/notifications/unread-count', 'GET /api/notifications/unread-count', 'notifications_unread_count', notificationsDuration, authHeaders, 'notifications success');
  getAuthenticated('/api/recommendations/me', 'GET /api/recommendations/me', 'recommendations_me', recommendationsDuration, authHeaders, 'recommendations success');

  sleep(Math.random() * 3 + 1);
}
