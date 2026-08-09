# Performance Tests

Install k6, then run against a non-production environment with synthetic test users.

Examples:

```bash
k6 run -e BASE_URL=http://localhost:8080 -e VUS=100 -e DURATION=5m performance/scripts/api-journey.js
k6 run -e BASE_URL=https://staging.example.com -e VUS=500 -e DURATION=2h performance/scripts/soak.js
k6 run -e BASE_URL=https://staging.example.com -e NORMAL_VUS=100 -e SPIKE_VUS=500 performance/scripts/spike.js
```

Do not run higher stages if the previous stage exceeds 5% errors, sustained p95 above 5 seconds, database exhaustion, or service unavailability.
