import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        simultaneous_load: {
            executor: 'constant-vus',
            vus: 1000,
            duration: '2m',
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