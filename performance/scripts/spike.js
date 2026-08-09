import journey from './api-journey.js';

const normalVus = Number(__ENV.NORMAL_VUS || '100');
const spikeVus = Number(__ENV.SPIKE_VUS || '500');

export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      stages: [
        { target: normalVus, duration: __ENV.NORMAL_DURATION || '5m' },
        { target: spikeVus, duration: __ENV.RAMP_DURATION || '1m' },
        { target: spikeVus, duration: __ENV.SPIKE_HOLD_DURATION || '5m' },
        { target: normalVus, duration: __ENV.RECOVERY_DURATION || '2m' },
        { target: 0, duration: __ENV.RAMP_DOWN_DURATION || '1m' }
      ]
    }
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<5000']
  }
};

export default journey;
