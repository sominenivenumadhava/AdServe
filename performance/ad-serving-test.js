import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// Custom metrics for AdServe platform
export const adServeDuration = new Trend('adserve_req_duration', true);
export const adServeSuccessRate = new Rate('adserve_success_rate');
export const adServeErrors = new Counter('adserve_errors');

// Scenarios configurations
const scenarios = {
  smoke: {
    executor: 'constant-vus',
    vus: 2,
    duration: '30s',
  },
  normal: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '15s', target: 10 },
      { duration: '30s', target: 25 },
      { duration: '30s', target: 50 },
      { duration: '15s', target: 0 },
    ],
  },
  stress: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '15s', target: 50 },
      { duration: '30s', target: 100 },
      { duration: '30s', target: 250 },
      { duration: '20s', target: 500 },
      { duration: '15s', target: 0 },
    ],
  },
  spike: {
    executor: 'ramping-vus',
    startVUs: 10,
    stages: [
      { duration: '10s', target: 10 },
      { duration: '10s', target: 200 }, // sudden spike
      { duration: '20s', target: 200 }, // sustained peak
      { duration: '10s', target: 10 },  // recovery
      { duration: '10s', target: 0 },
    ],
  },
  sustained: {
    executor: 'constant-vus',
    vus: 50,
    duration: '2m',
  },
  baseline: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '10s', target: 10 },
      { duration: '20s', target: 25 },
      { duration: '30s', target: 50 },
      { duration: '15s', target: 100 },
      { duration: '15s', target: 0 },
    ],
  },
};

const selectedScenarioName = __ENV.SCENARIO || 'baseline';
const activeScenario = scenarios[selectedScenarioName] || scenarios.baseline;

export const options = {
  scenarios: {
    ad_serve_load: activeScenario,
  },
  thresholds: {
    http_req_failed: ['rate<0.01'], // less than 1% failures
    http_req_duration: ['p(95)<500'], // 95% of requests under 500ms
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://adserve-backend:8080';
const TARGET_URL = `${BASE_URL}/api/ad-server/serve?country=IN&device=ANDROID&category=GAMING`;

export default function () {
  const params = {
    headers: {
      'Accept': 'application/json',
      'User-Agent': 'k6-load-test-agent',
    },
    timeout: '10s',
  };

  const startTime = Date.now();
  const res = http.get(TARGET_URL, params);
  const latency = Date.now() - startTime;

  adServeDuration.add(latency);

  let isSuccess = false;
  let hasValidAd = false;

  if (res.status === 200) {
    try {
      const body = JSON.parse(res.body);
      isSuccess = body.success === true && body.data !== null;
      hasValidAd = body.data && body.data.adId !== undefined && body.data.title !== undefined;
    } catch (e) {
      isSuccess = false;
    }
  }

  const passed = check(res, {
    'status is 200': (r) => r.status === 200,
    'response has success flag': () => isSuccess,
    'ad payload is valid': () => hasValidAd,
  });

  if (passed) {
    adServeSuccessRate.add(1);
  } else {
    adServeSuccessRate.add(0);
    adServeErrors.add(1);
  }

  // Brief think-time between user requests (10ms - 50ms) to simulate realistic client pacing
  sleep(0.02);
}
