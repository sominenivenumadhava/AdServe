# AdServe Platform — Load Testing & Performance Summary

This document summarizes the empirical performance metrics collected during **Phase 9: Load Testing & Performance Engineering**. All metrics were directly captured on the live containerized environment using **k6**, **Spring Boot Actuator**, **Redis Stats**, **MySQL EXPLAIN**, **Kafka Consumer CLI**, and **Docker Stats**.

---

## 1. Executive Performance Summary

| Test Scenario | Virtual Users (VUs) | Total Requests | Duration | Avg Latency | p50 (Median) | p90 Latency | p95 Latency | Throughput (RPS) | Error Rate |
|---|---|---|---|---|---|---|---|---|---|
| **Smoke Test** | 2 | 2,069 | 30s | 7.94 ms | 4.97 ms | 13.97 ms | 19.99 ms | 68.93 req/s | 0.00% |
| **Baseline Normal Load** | 10 → 25 → 50 | 50,068 | 90s | 19.41 ms | 11.64 ms | 45.46 ms | 63.03 ms | 556.31 req/s | 0.00% |
| **Optimized Normal Load** | 10 → 25 → 50 | 58,942 | 90s | **13.73 ms** | **8.98 ms** | **29.76 ms** | **41.33 ms** | **654.84 req/s** | 0.00% |
| **Stress Test** | 50 → 100 → 250 → 500 | 161,882 | 110s | 96.28 ms | 44.92 ms | 263.46 ms | 359.88 ms | 1,471.71 req/s | 0.00% |
| **Spike Test** | 10 → 200 → 10 | 85,438 | 60s | 51.57 ms | 20.83 ms | 134.50 ms | 177.28 ms | 1,424.04 req/s | 0.00% |
| **Sustained Load** | 50 constant | 192,216 | 120s | 9.98 ms | 5.87 ms | 20.63 ms | 30.79 ms | 1,601.72 req/s | 0.00% |
| **Kafka Event Pipeline** | 20 → 50 → 100 | 60,014 | 60s | 2.18 ms | 1.31 ms | 3.50 ms | 5.02 ms | 999.60 req/s | 0.00% |

---

## 2. Before vs. After Optimization

Targeted Optimization Applied:
1. **Database Index**: Added index `idx_ad_status` on `advertisements(status)`. Eliminated MySQL full table scan (`type: ALL` → `type: ref`).
2. **Transaction Scope**: Updated `serveAd` in `AdServingService` to `@Transactional(readOnly = true)`. Prevented unnecessary write transaction acquisition and connection pool contention on Redis cache hits.

| Metric | Baseline (Pre-Opt) | Optimized (Post-Opt) | Measured Delta |
|---|---|---|---|
| **Average Latency** | 19.41 ms | 13.73 ms | **29.3% reduction** |
| **Median (p50) Latency** | 11.64 ms | 8.98 ms | **22.9% reduction** |
| **p90 Latency** | 45.46 ms | 29.76 ms | **34.5% reduction** |
| **p95 Latency** | 63.03 ms | 41.33 ms | **34.4% reduction** |
| **Max Tail Latency** | 415.31 ms | 200.51 ms | **51.7% reduction** |
| **Throughput (RPS)** | 556.31 req/s | 654.84 req/s | **+17.7% throughput** |
| **Processed Requests (90s)** | 50,068 | 58,942 | **+8,874 requests** |
| **HTTP Error Rate** | 0.00% | 0.00% | 0.00% (No regressions) |

---

## 3. Redis In-Memory Caching Analysis

Controlled comparison of 50 consecutive requests with cold cache vs. warm cache:

| Metric | Scenario A: Cold Cache (Redis MISS → MySQL) | Scenario B: Warm Cache (Redis HIT) | Delta / Speedup |
|---|---|---|---|
| **Average Latency** | 78.08 ms | 5.62 ms | **13.89x faster** |
| **p95 Latency** | 96.00 ms | 18.00 ms | **5.33x faster** |
| **Min Latency** | 6.00 ms | 2.00 ms | **3.00x faster** |
| **Max Latency** | 2,888.00 ms | 51.00 ms | **56.63x faster** |
| **Database Queries / Req** | 1 complex joined query | 0 database queries | **100% DB offload** |
| **Cumulative Cache Hits** | — | 491,713 hits | — |
| **Cumulative Cache Misses**| — | 63 misses | — |
| **Overall Hit Rate** | — | **99.99%** | — |

---

## 4. Kafka Event Streaming & Consumer Lag

- **Producer Ingestion Throughput**: ~1,000 events/sec (`POST /api/ad-server/{id}/impression` and `/click`).
- **Producer Latency**: avg 2.18 ms, p95 5.02 ms.
- **Consumer Persistence Rate**: ~115–120 events/sec per consumer thread writing to MySQL.
- **Observed Peak Lag**: Under heavy stress traffic (500 VUs producing ~1,500 impressions/sec), consumer lag peaked at **35,228 events** on partition 0.
- **Lag Drain**: Once producer bursts subsided, consumers drained the backlog completely to 0 lag without event drops or duplicate processing.
- **Scaling Recommendation**: Partition traffic evenly across all 3 partitions using a hashed key (e.g. `adId`) and increase concurrency to 3 partition consumers (`concurrency = 3`).

---

## 5. Container Resource Utilization During Peak Load

Captured via `docker stats --no-stream`:

| Service Container | Peak CPU % | Memory Usage / Limit | Mem % | Network I/O (Total) | Block I/O |
|---|---|---|---|---|---|
| `adserve-backend` | 311.31% | 682.7 MiB / 3.717 GiB | 17.94% | 173 MB / 230 MB | 128 MB / 9.95 MB |
| `adserve-mysql` | 77.84% | 383.7 MiB / 3.717 GiB | 10.08% | 47.2 MB / 39.9 MB | 39.2 MB / 559 MB |
| `adserve-redis` | 16.07% | 7.172 MiB / 3.717 GiB | 0.19% | 18.4 MB / 77.6 MB | 14.8 MB / 8.19 kB |
| `adserve-kafka` | 71.63% | 548.4 MiB / 3.717 GiB | 14.41% | 127 MB / 30.8 MB | 170 MB / 255 MB |
| `adserve-frontend` | 0.00% | 8.129 MiB / 3.717 GiB | 0.21% | 11.1 kB / 253 kB | 7.18 MB / 8.19 kB |
