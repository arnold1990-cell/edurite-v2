import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Trend, Rate } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://localhost:5173';
const testEmailPrefix = __ENV.TEST_EMAIL_PREFIX || 'perf-user';
const testPassword = __ENV.TEST_PASSWORD || 'PerfTest@12345';
const perfTestUserCount = Number(__ENV.PERF_TEST_USER_COUNT || '100');
const vus = Number(__ENV.VUS || '50');
const duration = __ENV.DURATION || '1m';
const thinkTimeSeconds = Number(__ENV.THINK_TIME_SECONDS || '0');

const loginDuration = new Trend('endpoint_login_duration', true);
const loginFailureRate = new Rate('login_failed');

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
  scenarios: {
    login_burst: {
      executor: 'constant-vus',
      vus,
      duration,
      tags: { scenario: 'login_burst' }
    }
  },
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.05', abortOnFail: true, delayAbortEval: '30s' }],
    checks: ['rate>0.95']
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max']
};

export default function () {
  const userId = ((exec.vu.idInTest - 1) % perfTestUserCount) + 1;
  const email = `${testEmailPrefix}-${userId}@example.test`;
  const res = http.post(
    `${baseUrl}/api/auth/login`,
    JSON.stringify({ email, password: testPassword }),
    {
      headers: {
        'Content-Type': 'application/json',
        'X-Request-Id': `k6-login-burst-${exec.vu.idInTest}-${exec.scenario.iterationInTest}`
      },
      tags: { name: 'POST /api/auth/login', endpoint: 'auth_login', auth: 'none' }
    }
  );

  loginDuration.add(res.timings.duration);
  const ok = check(res, {
    'login success': r => r.status === 200,
    'token exists': r => {
      try {
        const token = r.json('accessToken');
        return typeof token === 'string' && token.length > 0;
      } catch (_) {
        return false;
      }
    }
  });
  loginFailureRate.add(!ok);

  if (thinkTimeSeconds > 0) {
    sleep(thinkTimeSeconds);
  }
}
