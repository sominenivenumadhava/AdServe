import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

export const impressionLatency = new Trend('impression_duration', true);
export const clickLatency = new Trend('click_duration', true);
export const pipelineSuccessRate = new Rate('pipeline_success_rate');

export const options = {
  scenarios: {
    event_pipeline_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 20 },
        { duration: '20s', target: 50 },
        { duration: '20s', target: 100 },
        { duration: '10s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<300'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://adserve-backend:8080';
const AD_ID = __ENV.AD_ID || '1';

export default function () {
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };

  // 1. Send Impression Beacon
  const impStart = Date.now();
  const impRes = http.post(`${BASE_URL}/api/ad-server/${AD_ID}/impression`, null, { headers, timeout: '5s' });
  impressionLatency.add(Date.now() - impStart);

  const impOk = check(impRes, {
    'impression status is 200': (r) => r.status === 200,
  });

  // 2. Simulate 10% Click-Through rate
  if (Math.random() < 0.15) {
    const clickStart = Date.now();
    const clickRes = http.post(`${BASE_URL}/api/ad-server/${AD_ID}/click`, null, { headers, timeout: '5s' });
    clickLatency.add(Date.now() - clickStart);

    const clickOk = check(clickRes, {
      'click status is 200': (r) => r.status === 200,
    });

    pipelineSuccessRate.add(impOk && clickOk ? 1 : 0);
  } else {
    pipelineSuccessRate.add(impOk ? 1 : 0);
  }

  sleep(0.05);
}
