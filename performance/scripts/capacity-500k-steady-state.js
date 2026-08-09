import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Trend, Rate } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://localhost:5173';
const targetVus = Number(__ENV.TARGET_VUS || __ENV.VUS || '500');
const warmupVus = Number(__ENV.WARMUP_VUS || Math.min(100, targetVus));
const warmupDuration = __ENV.WARMUP_DURATION || '5m';
const rampUpDuration = __ENV.RAMP_UP_DURATION || '30m';
const holdDuration = __ENV.HOLD_DURATION || '30m';
const rampDownDuration = __ENV.RAMP_DOWN_DURATION || '10m';
const allowDistributed = (__ENV.ALLOW_DISTRIBUTED_CAPACITY_TEST || '').toLowerCase() === 'true';

const testEmailPrefix = __ENV.TEST_EMAIL_PREFIX || 'perf-user';
const testPassword = __ENV.TEST_PASSWORD || 'PerfTest@12345';
const userIdStart = Number(__ENV.USER_ID_START || '1');
const usersPerGenerator = Number(__ENV.USERS_PER_GENERATOR || __ENV.PERF_TEST_USER_COUNT || '500');
const generatorIndex = Number(__ENV.GENERATOR_INDEX || '1');
const generatorCount = Number(__ENV.GENERATOR_COUNT || '1');
const tokenRefreshSkewMs = Number(__ENV.TOKEN_REFRESH_SKEW_SECONDS || '60') * 1000;
const minThinkSeconds = Number(__ENV.MIN_THINK_SECONDS || '8');
const maxThinkSeconds = Number(__ENV.MAX_THINK_SECONDS || '22');

const loginDuration = new Trend('capacity_login_duration', true);
const ordinaryApiDuration = new Trend('capacity_ordinary_api_duration', true);
const learnerDuration = new Trend('capacity_learner_duration', true);
const teacherDuration = new Trend('capacity_teacher_duration', true);
const schoolAdminDuration = new Trend('capacity_school_admin_duration', true);
const districtDuration = new Trend('capacity_district_duration', true);
const careerHeavyDuration = new Trend('capacity_career_heavy_duration', true);
const adminOtherDuration = new Trend('capacity_admin_other_duration', true);
const loginFailures = new Rate('capacity_login_failed');

let accessToken = null;
let refreshToken = null;
let accessTokenExpiresAt = 0;

function validateConfig() {
  if (!Number.isInteger(targetVus) || targetVus < 1) {
    throw new Error(`TARGET_VUS/VUS must be a positive integer. Received ${__ENV.TARGET_VUS || __ENV.VUS}`);
  }
  if (!Number.isInteger(usersPerGenerator) || usersPerGenerator < 1) {
    throw new Error(`USERS_PER_GENERATOR/PERF_TEST_USER_COUNT must be positive. Received ${__ENV.USERS_PER_GENERATOR || __ENV.PERF_TEST_USER_COUNT}`);
  }
  if (!Number.isInteger(userIdStart) || userIdStart < 1) {
    throw new Error(`USER_ID_START must be positive. Received ${__ENV.USER_ID_START}`);
  }
  if (!Number.isInteger(generatorIndex) || generatorIndex < 1 || !Number.isInteger(generatorCount) || generatorCount < 1) {
    throw new Error('GENERATOR_INDEX and GENERATOR_COUNT must be positive integers.');
  }
  if (targetVus > usersPerGenerator) {
    throw new Error(`This generator requested ${targetVus} VUs but only ${usersPerGenerator} users are assigned.`);
  }
  if (targetVus > 500 && /localhost|127\.0\.0\.1/.test(baseUrl)) {
    throw new Error(`Refusing ${targetVus} local VUs against ${baseUrl}. Use a staging load balancer and distributed generators.`);
  }
  if (targetVus >= 50000 && !allowDistributed) {
    throw new Error('Large capacity stages require ALLOW_DISTRIBUTED_CAPACITY_TEST=true in a dedicated performance environment.');
  }
}

validateConfig();

export const options = {
  scenarios: {
    steady_state_capacity: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: warmupDuration, target: warmupVus },
        { duration: rampUpDuration, target: targetVus },
        { duration: holdDuration, target: targetVus },
        { duration: rampDownDuration, target: 0 }
      ],
      gracefulRampDown: '2m',
      tags: {
        scenario: 'capacity_steady_state',
        generator_index: String(generatorIndex),
        generator_count: String(generatorCount)
      }
    }
  },
  thresholds: {
    'http_req_failed{phase:steady_state}': [{ threshold: 'rate<0.01', abortOnFail: true, delayAbortEval: '2m' }],
    'capacity_ordinary_api_duration': [{ threshold: 'p(95)<1000', abortOnFail: true, delayAbortEval: '5m' }],
    checks: ['rate>0.99']
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max']
};

function assignedUserId() {
  return userIdStart + ((exec.vu.idInTest - 1) % usersPerGenerator);
}

function roleForUser(userId) {
  const bucket = userId % 100;
  if (bucket < 70) return 'learner';
  if (bucket < 85) return 'teacher';
  if (bucket < 93) return 'school_admin';
  if (bucket < 97) return 'district_curriculum';
  if (bucket < 99) return 'career_guidance_heavy';
  return 'admin_other';
}

function login() {
  const userId = assignedUserId();
  const res = http.post(`${baseUrl}/api/auth/login`, JSON.stringify({
    email: `${testEmailPrefix}-${userId}@example.test`,
    password: testPassword
  }), {
    headers: { 'Content-Type': 'application/json', 'X-Request-Id': `capacity-login-${generatorIndex}-${userId}` },
    tags: { name: 'POST /api/auth/login', endpoint: 'auth_login', phase: 'login_ramp' }
  });
  loginDuration.add(res.timings.duration);
  const ok = check(res, { 'login success': r => r.status === 200 });
  loginFailures.add(!ok);
  if (!ok) return false;

  accessToken = res.json('accessToken');
  refreshToken = res.json('refreshToken');
  accessTokenExpiresAt = Date.now() + Number(res.json('accessTokenExpiresIn') || 3600) * 1000;
  return check(accessToken, { 'token exists': t => typeof t === 'string' && t.length > 0 });
}

function refreshOrLogin() {
  if (!accessToken) return login();
  if (Date.now() + tokenRefreshSkewMs < accessTokenExpiresAt) return true;
  if (!refreshToken) return login();

  const res = http.post(`${baseUrl}/api/auth/refresh`, JSON.stringify({ refreshToken }), {
    headers: { 'Content-Type': 'application/json', 'X-Request-Id': `capacity-refresh-${generatorIndex}-${assignedUserId()}` },
    tags: { name: 'POST /api/auth/refresh', endpoint: 'auth_refresh', phase: 'token_refresh' }
  });
  if (res.status !== 200) return login();
  accessToken = res.json('accessToken');
  refreshToken = res.json('refreshToken') || refreshToken;
  accessTokenExpiresAt = Date.now() + Number(res.json('accessTokenExpiresIn') || 3600) * 1000;
  return true;
}

function get(path, endpoint, trend) {
  const res = http.get(`${baseUrl}${path}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'X-Request-Id': `capacity-${generatorIndex}-${assignedUserId()}-${exec.scenario.iterationInTest}`
    },
    tags: { name: `GET ${path}`, endpoint, phase: 'steady_state' }
  });
  trend.add(res.timings.duration);
  ordinaryApiDuration.add(res.timings.duration);
  check(res, { [`${endpoint} success`]: r => r.status >= 200 && r.status < 300 });
  sleep(Math.random() * (maxThinkSeconds - minThinkSeconds) + minThinkSeconds);
}

function learnerJourney() {
  get('/api/account/me', 'account_me', learnerDuration);
  get('/api/courses?page=0&size=20', 'courses', learnerDuration);
  get('/api/bursaries?page=0&size=20', 'bursaries', learnerDuration);
  get('/api/recommendations/me', 'recommendations_me', learnerDuration);
}

function teacherJourney() {
  get('/api/account/me', 'account_me', teacherDuration);
  get('/api/courses?page=0&size=20', 'courses', teacherDuration);
  get('/api/notifications/unread-count', 'notifications_unread_count', teacherDuration);
}

function schoolAdminJourney() {
  get('/api/account/me', 'account_me', schoolAdminDuration);
  get('/api/notifications/unread-count', 'notifications_unread_count', schoolAdminDuration);
  get('/api/courses?page=0&size=20', 'courses', schoolAdminDuration);
}

function districtJourney() {
  get('/api/account/me', 'account_me', districtDuration);
  get('/api/careers?page=0&size=20', 'careers', districtDuration);
  get('/api/courses?page=0&size=20', 'courses', districtDuration);
}

function careerGuidanceHeavyJourney() {
  get('/api/careers?page=0&size=20', 'careers', careerHeavyDuration);
  get('/api/courses?page=0&size=20', 'courses', careerHeavyDuration);
  get('/api/bursaries?page=0&size=20', 'bursaries', careerHeavyDuration);
  get('/api/recommendations/me', 'recommendations_me', careerHeavyDuration);
}

function adminOtherJourney() {
  get('/api/account/me', 'account_me', adminOtherDuration);
  get('/api/notifications/unread-count', 'notifications_unread_count', adminOtherDuration);
}

export default function () {
  if (!refreshOrLogin()) {
    sleep(5);
    return;
  }

  const role = roleForUser(assignedUserId());
  if (role === 'learner') learnerJourney();
  else if (role === 'teacher') teacherJourney();
  else if (role === 'school_admin') schoolAdminJourney();
  else if (role === 'district_curriculum') districtJourney();
  else if (role === 'career_guidance_heavy') careerGuidanceHeavyJourney();
  else adminOtherJourney();
}
