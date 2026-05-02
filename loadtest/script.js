import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },   // 워밍업
    { duration: '1m',  target: 50 },   // 부하 증가
    { duration: '30s', target: 0  },   // 종료
  ],
  thresholds: {
    http_req_failed:   ['rate<0.01'],   // 에러율 1% 미만
    http_req_duration: ['p(95)<500'],   // p95 500ms 이하
  },
};

export default function () {
  const res = http.get('http://localhost:8080/actuator/health');
  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(1);
}
