# Distributed Load Testing

A single workstation cannot normally generate or sustain a real 500,000-user test.

## Options

- Grafana Cloud k6 for managed distributed runs.
- Kubernetes k6 Operator with many load-generator pods.
- Multiple self-managed k6 generators coordinated by stage and result aggregation.

## Method

1. Run the same scenario from one generator and record generator CPU, memory, network, and max stable VUs.
2. Calculate required generator count from measured generator capacity.
3. Add 30% spare generator capacity.
4. Increase target stages only when the previous stage meets abort thresholds.

Do not estimate final generator count before measuring generator capacity.
