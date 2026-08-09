import journey, { options as journeyOptions } from './api-journey.js';

export const options = {
  ...journeyOptions,
  scenarios: {
    soak: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || '500'),
      duration: __ENV.DURATION || '2h'
    }
  }
};

export default journey;
