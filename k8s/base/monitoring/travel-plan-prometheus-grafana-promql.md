# Travel-Plan --- Prometheus & Grafana Query Cookbook

A practical PromQL reference for monitoring the Travel-Plan Spring Boot
microservices and Kubernetes autoscaling.

> **Important:** PromQL queries only work when the referenced metric
> exists and its labels match your installation. Start in **Grafana →
> Explore → Prometheus** and use the metric browser or run
> `process_cpu_usage` and `http_server_requests_seconds_count` to
> inspect available labels.

## 1. Start with the metrics you already have

### HTTP request throughput (requests/second)

``` promql
sum(rate(http_server_requests_seconds_count[1m]))
```

Shows the total request rate across all scraped application instances.

### Request rate by application/service

``` promql
sum by (application) (rate(http_server_requests_seconds_count[1m]))
```

If your label is called `service` or `job` instead of `application`,
replace the grouping label accordingly. To discover labels, run:

``` promql
http_server_requests_seconds_count
```

### Process CPU usage

``` promql
process_cpu_usage
```

Spring Boot's `process_cpu_usage` is typically a ratio from `0` to `1`
(for example, `0.25` means 25% of one CPU's capacity). It is **not the
same metric** as Kubernetes HPA CPU utilization, which is calculated
relative to container CPU requests.

Average process CPU by application, if the label exists:

``` promql
avg by (application) (process_cpu_usage)
```

## 2. HTTP traffic and performance

### Request rate by URI

``` promql
sum by (uri) (rate(http_server_requests_seconds_count[1m]))
```

Use this to identify which routes receive the most traffic. Avoid
grouping by raw URL values that contain IDs or other unbounded values;
prefer Spring's templated `uri` labels such as `/api/travels/{id}`.

### Request rate by HTTP status

``` promql
sum by (status) (rate(http_server_requests_seconds_count[5m]))
```

Useful for seeing the mix of successful and failed HTTP responses.

### 5xx response rate

``` promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
```

### 5xx error percentage

``` promql
100 *
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/
clamp_min(sum(rate(http_server_requests_seconds_count[5m])), 1e-9)
```

If no requests are occurring, this expression avoids division by zero. A
value of `0` means no 5xx responses were observed in the selected
window.

### 4xx response rate

``` promql
sum(rate(http_server_requests_seconds_count{status=~"4.."}[5m]))
```

4xx responses may be expected (for example, validation errors or
unauthenticated requests); interpret them in application context.

### Request latency --- p95 (requires histogram buckets)

``` promql
histogram_quantile(
  0.95,
  sum by (le) (rate(http_server_requests_seconds_bucket[5m]))
)
```

Estimates the 95th-percentile request duration: 95% of observed requests
completed at or below this duration. The result is in seconds.

### Request latency --- p95 by URI (requires histogram buckets)

``` promql
histogram_quantile(
  0.95,
  sum by (uri, le) (rate(http_server_requests_seconds_bucket[5m]))
)
```

Use templated URI labels to avoid high-cardinality dashboards.

### Average request latency (requires `_sum` and `_count`)

``` promql
sum(rate(http_server_requests_seconds_sum[5m]))
/
clamp_min(sum(rate(http_server_requests_seconds_count[5m])), 1e-9)
```

This is average duration in seconds. It is not a substitute for p95/p99
latency because averages can hide slow requests.

### Requests currently in flight (if exported)

``` promql
http_server_requests_active_seconds_count
```

This metric name varies by Spring Boot/Micrometer version and
configuration. Search the metric browser for `active` or `in_flight`
first.

## 3. JVM and process health

These are common Spring Boot Actuator/Micrometer metrics. Use them only
if they are present in your Prometheus metric browser.

### JVM heap used (bytes)

``` promql
sum by (application) (jvm_memory_used_bytes{area="heap"})
```

### JVM heap used as a percentage of heap maximum

``` promql
100 *
sum by (application) (jvm_memory_used_bytes{area="heap"})
/
clamp_min(sum by (application) (jvm_memory_max_bytes{area="heap"}), 1)
```

Some memory pools may report an undefined maximum. Check the returned
series before relying on this panel.

### Live JVM threads

``` promql
sum by (application) (jvm_threads_live_threads)
```

### Garbage collection rate

``` promql
sum by (application) (rate(jvm_gc_pause_seconds_count[5m]))
```

This is the number of GC pause events per second, not the time spent
paused.

### Time spent in GC pauses per second

``` promql
sum by (application) (rate(jvm_gc_pause_seconds_sum[5m]))
```

For example, `0.02` means approximately 20 milliseconds of aggregate GC
pause time per second across the grouped instances.

### Process CPU as a percentage

``` promql
100 * process_cpu_usage
```

This is suitable for a percentage-style Grafana panel if the metric is a
0--1 ratio. It may exceed 100% only for metrics with different
semantics; verify your exporter before assuming that.

## 4. Database and connection pools (if exported)

### HikariCP active connections

``` promql
sum by (application) (hikaricp_connections_active)
```

### HikariCP maximum connections

``` promql
sum by (application) (hikaricp_connections_max)
```

### Connection-pool usage percentage

``` promql
100 *
sum by (application) (hikaricp_connections_active)
/
clamp_min(sum by (application) (hikaricp_connections_max), 1)
```

If your services use a different pool or metric names, search for
`connections_active`, `connections_max`, or `hikaricp`.

## 5. Kubernetes autoscaling and pod health

These queries require Prometheus to scrape the relevant Kubernetes
metrics, commonly from `kube-state-metrics` and/or cAdvisor. They are
**not guaranteed** to exist just because Spring Boot metrics are
available.

### Current desired replicas by deployment

``` promql
kube_deployment_spec_replicas{namespace="travel-plan"}
```

### Available replicas by deployment

``` promql
kube_deployment_status_replicas_available{namespace="travel-plan"}
```

### CPU usage by pod (cores)

``` promql
sum by (pod) (
  rate(container_cpu_usage_seconds_total{
    namespace="travel-plan",
    container!="",
    container!="POD"
  }[5m])
)
```

### Container CPU usage as a percentage of CPU request

``` promql
100 *
sum by (namespace, pod, container) (
  rate(container_cpu_usage_seconds_total{
    namespace="travel-plan",
    container!="",
    container!="POD"
  }[5m])
)
/
clamp_min(
  sum by (namespace, pod, container) (
    kube_pod_container_resource_requests{namespace="travel-plan", resource="cpu", unit="core"}
  ),
  1e-9
)
```

This is a per-container ratio of CPU cores used to CPU cores requested.
It can differ from the HPA's reported average utilization due to
aggregation, missing series, and scrape timing. Check your metric labels
and metric versions before using it as an exact HPA replica.

### Pod restarts

``` promql
sum by (pod) (
  increase(kube_pod_container_status_restarts_total{namespace="travel-plan"}[15m])
)
```

### Pods not ready

``` promql
kube_pod_status_ready{namespace="travel-plan", condition="false"}
```

For HPA decisions, compare these Prometheus charts with the
authoritative Kubernetes view:

``` bash
sudo kubectl get hpa -n travel-plan -w
sudo kubectl get pods -n travel-plan -w
```

## 6. Recommended Grafana dashboard layout

A useful first dashboard can be organized into rows:

### Row A --- Traffic

-   Total request rate:
    `sum(rate(http_server_requests_seconds_count[1m]))`
-   Request rate by service/application
-   Request rate by status code

### Row B --- Latency and errors

-   p95 request latency (if `_bucket` exists)
-   Average request latency
-   5xx error percentage

### Row C --- Application runtime

-   Process CPU usage
-   JVM heap used percentage
-   Live JVM threads
-   GC pause time

### Row D --- Kubernetes scaling

-   Desired vs available replicas
-   CPU usage by pod
-   Pod restarts and readiness
-   HPA replica count from Kubernetes CLI alongside Grafana charts

## 7. Suggested alert starting points

Tune these thresholds after collecting a baseline; they are starting
points, not universal production standards.

-   **5xx error rate:** alert if the 5xx percentage stays above 2% for 5
    minutes, provided request volume is meaningful.
-   **p95 latency:** alert when p95 exceeds your service-level objective
    for 5 minutes.
-   **CPU:** alert on sustained high CPU, but remember HPA scaling may
    be the intended response.
-   **Restarts:** alert on repeated container restarts within 15
    minutes.
-   **Unavailable replicas:** alert when available replicas remain below
    desired replicas for several minutes.

Example PromQL condition for a 5xx percentage above 2%:

``` promql
(
  100 *
  sum(rate(http_server_requests_seconds_count{namespace="travel-plan", status=~"5.."}[5m]))
  /
  clamp_min(sum(rate(http_server_requests_seconds_count{namespace="travel-plan"}[5m])), 1e-9)
) > 2
```

Remove `namespace="travel-plan"` if that label does not exist on your
application metrics; use the labels actually present.

## 8. Troubleshooting missing metrics

-   If `http_server_requests_seconds_count` returns data but the latency
    query does not, check whether `http_server_requests_seconds_bucket`
    exists. Histogram buckets must be enabled/exported for
    `histogram_quantile` to work.
-   If grouping by `application` returns no data, inspect the raw series
    and use the actual label (often `job`, `instance`, or a configured
    `application` label).
-   If Kubernetes queries return no data, verify that
    kube-state-metrics/cAdvisor data is being scraped and that metric
    names/labels match your Prometheus version.
-   Use a Grafana dashboard time range that includes the test period and
    a refresh interval such as 5--10 seconds during a load test.
-   Avoid using high-cardinality labels such as raw URLs, user IDs,
    email addresses, or request IDs in dashboard groupings.

## 9. Minimum query set for your first load test

Start with these four panels:

**Request rate**

``` promql
sum(rate(http_server_requests_seconds_count[1m]))
```

**Process CPU**

``` promql
100 * process_cpu_usage
```

**p95 latency** (only if histogram buckets exist)

``` promql
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[5m])))
```

**Available application replicas** (only if Kubernetes state metrics
exist)

``` promql
kube_deployment_status_replicas_available{namespace="travel-plan"}
```

Use these alongside `kubectl get hpa -n travel-plan -w`. A load test is
successful when you can correlate traffic with CPU, latency, errors, and
replica changes---not merely when replica count increases.
