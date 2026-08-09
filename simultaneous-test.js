import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        fifty_thousand_users: {
            executor: 'ramping-vus',
            startVUs: 1000,
            stages: [
                { duration: '30s', target: 5000 },
                { duration: '30s', target: 10000 },
                { duration: '30s', target: 25000 },
                { duration: '30s', target: 50000 },

                // Hold 50,000 concurrent VUs
                { duration: '2m', target: 50000 },

                // Ramp down
                { duration: '30s', target: 0 },
            ],
            gracefulRampDown: '30s',
        },
    },

    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000'],
    },
};

export default function () {
    const res = http.get('http://localhost:8080/actuator/health');

    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    sleep(1);
}