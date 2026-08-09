import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://localhost:5173';
const testEmailPrefix = __ENV.TEST_EMAIL_PREFIX || 'perf-user';
const testPassword = __ENV.TEST_PASSWORD || 'PerfTest@12345';
const perfTestUserCount = Number(__ENV.PERF_TEST_USER_COUNT || '100');
const vus = Number(__ENV.VUS || '100');
const duration = __ENV.DURATION || '1m';

const readinessDuration = new Trend('endpoint_readiness_duration', true);
const careersDuration = new Trend('endpoint_careers_duration', true);
const coursesDuration = new Trend('endpoint_courses_duration', true);
const bursariesDuration = new Trend('endpoint_bursaries_duration', true);
const accountMeDuration = new Trend('endpoint_account_me_duration', true);
const notificationsDuration = new Trend('endpoint_notifications_unread_count_duration', true);
const recommendationsDuration = new Trend('endpoint_recommendations_duration', true);
const authenticatedApiDuration = new Trend('authenticated_api_duration', true);

if (!Number.isInteger(perfTestUserCount) || perfTestUserCount < 1 || perfTestUserCount > 50000) {
  throw new Error(`PERF_TEST_USER_COUNT must be an integer from 1 to 50000. Received: ${__ENV.PERF_TEST_USER_COUNT}`);
}

if (!Number.isInteger(vus) || vus < 1) {
  throw new Error(`VUS must be a positive integer. Received: ${__ENV.VUS}`);
}

if (vus > perfTestUserCount) {
  throw new Error(`Configuration error: requested ${vus} VUs but PERF_TEST_USER_COUNT=${perfTestUserCount}.`);
}

export const options = {
  setupTimeout: __ENV.PREAUTH_SETUP_TIMEOUT || '10m',
  scenarios: {
    authenticated_api_only: {
      executor: 'constant-vus',
      vus,
      duration,
      tags: { scenario: 'authenticated_api_only' }
    }
  },
  thresholds: {
    'http_req_failed{phase:measured}': [{ threshold: 'rate<0.05', abortOnFail: true, delayAbortEval: '30s' }],
    'http_req_duration{phase:measured}': [{ threshold: 'p(95)<5000', abortOnFail: true, delayAbortEval: '30s' }],
    checks: ['rate>0.95'],
    'checks{check:readiness 200}': [{ threshold: 'rate>0.99', abortOnFail: true, delayAbortEval: '15s' }]
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max']
};

export function setup() {
  const tokens = [];

  for (let userId = 1; userId <= vus; userId += 1) {
    const email = `${testEmailPrefix}-${userId}@example.test`;
    const res = http.post(
      `${baseUrl}/api/auth/login`,
      JSON.stringify({ email, password: testPassword }),
      {
        headers: {
          'Content-Type': 'application/json',
          'X-Request-Id': `k6-preauth-${userId}`
        },
        tags: { name: 'POST /api/auth/login preauth', endpoint: 'auth_login_preauth', auth: 'none', phase: 'setup' }
      }
    );

    if (res.status !== 200) {
      throw new Error(`Pre-authentication failed for ${email}: status=${res.status} body=${res.body}`);
    }

    const token = res.json('accessToken');
    if (typeof token !== 'string' || token.length === 0) {
      throw new Error(`Pre-authentication did not return accessToken for ${email}`);
    }
    tokens.push(token);
  }

  return { tokens };
}

function getReadiness() {
  const res = http.get(`${baseUrl}/actuator/health/readiness`, {
    tags: { name: 'GET readiness', endpoint: 'readiness', auth: 'none', phase: 'measured' }
  });
  readinessDuration.add(res.timings.duration);
  check(res, { 'readiness 200': r => r.status === 200 });
}

function getAuthenticated(path, name, endpoint, trend, headers, expectedCheckName) {
  const res = http.get(`${baseUrl}${path}`, {
    headers,
    tags: { name, endpoint, auth: 'bearer', phase: 'measured' }
  });
  trend.add(res.timings.duration);
  authenticatedApiDuration.add(res.timings.duration);
  check(res, { [expectedCheckName]: r => r.status >= 200 && r.status < 300 });
}

export default function (data) {
  const token = data.tokens[exec.vu.idInTest - 1];
  if (!token) {
    throw new Error(`No pre-authenticated token for VU ${exec.vu.idInTest}`);
  }

  const authHeaders = {
    Authorization: `Bearer ${token}`,
    'X-Request-Id': `k6-auth-api-${exec.vu.idInTest}-${exec.scenario.iterationInTest}`
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
