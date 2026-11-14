// k6 转账压测：阶段一 500 TPS / 阶段二 5000 TPS 目标
// 运行：k6 run -e BASE=http://localhost:8080 benchmark/k6/transfer.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');
const BASE = __ENV.BASE || 'http://localhost:8080';

export const options = {
  stages: [
    { duration: '30s', target: 100 },
    { duration: '2m',  target: 500 },
    { duration: '5m',  target: 5000 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(99)<200'],
    errors: ['rate<0.01'],
  },
};

export default function () {
  const payer = 10001 + Math.floor(Math.random() * 100);
  let payee = 10001 + Math.floor(Math.random() * 100);
  if (payee === payer) payee = 10001;
  const payload = JSON.stringify({
    tenantId: 1, payerAccountId: payer, payeeAccountId: payee,
    amountMinor: 100, currency: 'CNY',
  });
  const params = { headers: { 'Content-Type': 'application/json',
      'Idempotency-Key': `k6-${__VU}-${__ITER}` } };
  const res = http.post(`${BASE}/v1/transactions`, payload, params);
  check(res, { 'status 200': (r) => r.status === 200 });
  errorRate.add(res.status !== 200);
  sleep(0.01);
}