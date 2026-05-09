import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.APP_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    constant_load: {
      executor: 'constant-arrival-rate',
      rate: 200,
      timeUnit: '1s',
      duration: '15m',
      preAllocatedVUs: 100,
      maxVUs: 500,
    },
  },
  thresholds: {
    http_req_failed:   ['rate<0.05'],
    http_req_duration: ['p(95)<5000'],
  },
};

export default function () {
  const customerId = Math.floor(Math.random() * 10000) + 1;
  const productId  = Math.floor(Math.random() * 1000)  + 1;

  const res = http.post(`${BASE_URL}/api/orders`, JSON.stringify({
    customerId, productId, quantity: 1,
  }), { headers: { 'Content-Type': 'application/json' } });

  check(res, {
    'status 201': (r) => r.status === 201,
    'has orderId': (r) => JSON.parse(r.body).orderId !== undefined,
  });
  sleep(0);
}
