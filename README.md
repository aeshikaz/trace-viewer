# Trace Viewer

A simplified distributed tracing tool, inspired by tools like **Jaeger** and
**Zipkin**. When one request triggers multiple backend services, this tool
stitches together each service's logs into a single visual timeline — so
instead of manually digging through separate log files to find out why a
request was slow, you paste in one trace ID and see exactly where the time
went.

This is a learning/portfolio project, not a production-grade tracing system.
See "What I simplified" below for an honest breakdown of the gaps between
this and the real thing.

## The problem this solves

In a real backend, one user action often triggers several small services
working together. For example: placing an order might touch a
login-check service, a pricing service, a payment service, and a
notification service — four separate programs, each keeping its own logs.

If a customer says "my order took 10 seconds," an engineer has to figure out
which of those four services was slow. Comparing timestamps across four
separate log files by hand doesn't scale once there are hundreds of requests
happening per second — you need a way to isolate the log lines belonging to
*one specific request* and see them as a single timeline.

This tool automates that: every request gets a unique **trace ID** the
moment it starts, every service tags its log lines with that same ID, and a
dashboard lets you look up a trace ID and see a bar chart of how long each
service took.

**Who uses this**: not the end customer — this is an internal tool for
engineers debugging their own systems, the same way a mechanic uses a
diagnostic scanner that drivers never see.

## How it works

1. A `front-door` service receives a request and generates a unique trace ID
2. It calls four downstream services in sequence, passing that same trace ID
   to each one
3. Each service logs when it starts and finishes, tagged with the trace ID
4. A **collector** reads every service's log file and groups lines by trace ID
5. A **dashboard** lets you search a trace ID and renders a bar chart showing
   how long each service took, in order — the slowest one is highlighted

## The demo scenario

To generate realistic multi-service traffic to trace, I simulated a small
e-commerce-style backend with 5 fake services. **These services don't do any
real work** — they're a believable example scenario built specifically to
give the tracing tool something to demonstrate on. The actual engineering
substance of this project is the tracing/correlation/visualization logic
(`TraceLogger`, `Collector`, `DashboardServer`), not the fake services
themselves.

| Service | Port | Simulates | Real work done |
|---|---|---|---|
| `front-door` | 8080 | The entry point (e.g. "Place Order") | Generates the trace ID, calls the other 4 in sequence |
| `check-user` | 8081 | Verifying a logged-in user | None — sleeps ~100-300ms to simulate latency |
| `check-price` | 8082 | Calculating an order total | None — sleeps ~100-300ms |
| `payment` | 8084 | Charging a card | None — sleeps ~2-4 seconds (deliberately slow, since payment calls to external providers are a realistic bottleneck) |
| `send-confirmation` | 8083 | Sending a confirmation email/SMS | None — sleeps ~50-150ms |

All durations shown on the dashboard are **actually measured**
(`System.currentTimeMillis()` before and after each operation) — not
hardcoded numbers, even though the "work" being timed is a simulated sleep
rather than a real database call or API request.

## Project structure

```
trace-viewer/
├── pom.xml
├── src/main/java/com/traceviewer/
│   ├── common/TraceLogger.java        # writes JSON log lines to logs/<service>.log
│   ├── services/
│   │   ├── FrontDoorService.java      # entry point, port 8080
│   │   ├── CheckUserService.java      # port 8081
│   │   ├── CheckPriceService.java     # port 8082
│   │   ├── SendConfirmationService.java # port 8083
│   │   └── PaymentService.java        # port 8084
│   ├── collector/
│   │   ├── Collector.java             # reads logs, groups by trace_id
│   │   └── TraceSpan.java             # one service's contribution to a trace
│   └── dashboard/
│       └── DashboardServer.java       # port 9090, serves API + website
└── src/main/resources/static/index.html  # the dashboard page
```

No external libraries are used — everything runs on the plain JDK
(`com.sun.net.httpserver` for the mini web servers, `java.net.http.HttpClient`
for service-to-service calls, hand-parsed JSON for logs). This means there's
nothing to download to build it beyond a JDK.

## How to run it

You need **JDK 17 or newer**. Check with `java -version`.

Create an empty `logs/` folder at the project root first (this is where log
files get written — it needs to exist before the services run).

### Using VS Code

1. Open the project folder in VS Code with the **"Extension Pack for Java"**
   installed
2. Run each of these files individually, in this order, using the
   **"Run main"** link that appears above each `main` method (each in its own
   terminal, so they all keep running simultaneously):
   1. `CheckUserService.java`
   2. `CheckPriceService.java`
   3. `PaymentService.java`
   4. `SendConfirmationService.java`
   5. `FrontDoorService.java`
   6. `DashboardServer.java`

You should end up with **6 terminals running** at once.

### From a plain terminal

```bash
javac -d out $(find src -name "*.java")

# then in 6 separate terminals, run from the project root:
java -cp out com.traceviewer.services.CheckUserService
java -cp out com.traceviewer.services.CheckPriceService
java -cp out com.traceviewer.services.PaymentService
java -cp out com.traceviewer.services.SendConfirmationService
java -cp out com.traceviewer.services.FrontDoorService
java -cp out com.traceviewer.dashboard.DashboardServer
```

Run everything from the project root — the services write to `logs/` and the
dashboard reads `src/main/resources/static/index.html` using relative paths.

## Trying it out

1. With all 6 running, visit `http://localhost:8080/place-order` in a browser
   — you'll get back a trace ID, e.g. `{"traceId":"a1b2c3d4"}`
2. Open `http://localhost:9090`, paste that trace ID in, click "Look up"
3. You'll see a bar per service — `payment` should stand out in red as the
   real bottleneck, with `front-door` shown separately in gray as the overall
   total (see below for why it's treated differently)
4. Refresh `place-order` a few more times and look up different trace IDs —
   the numbers change each time since the simulated delays are randomized

## What I simplified vs. real tools like Jaeger/OpenTelemetry

- **Flat spans instead of a true parent-child tree.** In a real tracing
  system, `front-door`'s span would be the *parent* of the other four, drawn
  as a container around them — because it isn't independent work, it's the
  total of everything it called. I initially treated all 5 spans as equal
  rows, which meant `front-door` won "slowest" by definition, since its
  duration is the sum of the other four. I fixed the dashboard to exclude
  `front-door` from the "which one is the bottleneck" calculation and render
  it as a visually distinct "total" bar instead — but the underlying data
  model still doesn't capture real parent/child span relationships or
  support concurrent/overlapping child spans, which real tools do.
- **No sampling** — real tracing systems only capture a fraction of traces
  under high traffic, to save storage. This captures every single request.
- **File-based storage** — real systems use storage engines built for fast
  queries over millions of traces (e.g. Elasticsearch, Cassandra). This
  reads flat log files line by line, which doesn't scale past a demo.
- **No automatic trace ID propagation** — real systems use standard headers
  (like W3C Trace Context) so trace IDs pass through automatically. Here
  it's manually passed as a query parameter.
- **Hand-parsed JSON** — a real project would use a JSON library; this
  hand-rolls parsing with regex since the log format is small and fixed, to
  avoid needing external dependencies.
- **Sequential, not parallel, calls** — `front-door` calls the four services
  one after another. A real system might call some of them in parallel,
  which would require the dashboard to render overlapping bars correctly.
