import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

// custom counters
const allowedRequests = new Counter('allowed_requests');
const deniedRequests  = new Counter('denied_requests');

export const options = {
    vus:      100,   // 100 virtual users (concurrent)
    duration: '10s', // run for 10 seconds
};

export default function () {
    const url     = 'http://localhost:8080/api/v1/rate-limiter/check';
    const payload = JSON.stringify({
        tenantId: 'tenant-pro',
        userId:   `user-${__VU}`,   // each virtual user has its own ID
        action:   'ai_generate',
        cost:     1,
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-API-Key':    'sk_socials_abc123',
        },
    };

    const response = http.post(url, payload, params);

    // verify we always get a valid response
    check(response, {
        'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
        'response has allowed field': (r) => JSON.parse(r.body).allowed !== undefined,
    });

    // track allowed vs denied
    const body = JSON.parse(response.body);
    if (body.allowed) {
        allowedRequests.add(1);
    } else {
        deniedRequests.add(1);
    }

    sleep(0.1); // 100ms between requests per user
}