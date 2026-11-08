# Carrier Route Dispatcher

A Java concurrency deep-dive project demonstrating the evolution from **JDK 17 platform threads** to **JDK 21 virtual threads** through a real-world delivery order dispatching system.

## 🎯 What This Project Demonstrates

Instead of dealing with the complexity of systems like Kafka, this project focuses purely on **thread management**, **shared state**, and **scaling bottlenecks** — making it ideal for understanding and explaining Java concurrency in interviews.

## 📐 Architecture

```
src/main/java/
├── Main.java                          # CLI entry point
├── model/
│   └── DeliveryOrder.java             # Record with orderId, destination, Status enum
├── state/
│   └── OrderStateManager.java         # ConcurrentHashMap-backed thread-safe state
├── task/
│   └── DeliveryTask.java              # Runnable: PENDING → IN_TRANSIT → DELIVERED
├── dispatcher/
│   └── RouteDispatcher.java           # Submits tasks to any ExecutorService
└── runner/
    ├── Phase1Runner.java              # FixedThreadPool, CPU-only         (JDK 17)
    ├── Phase2Runner.java              # FixedThreadPool, I/O bottleneck   (JDK 17)
    └── Phase3Runner.java              # Virtual Threads, massive I/O      (JDK 21)
```

## 🚀 Phases

### Phase 1: JDK 17 — Fixed Thread Pool + Shared State
- **10 platform threads** process **100 orders** with CPU-only workload
- Demonstrates safe concurrent writes via `ConcurrentHashMap`
- **Result:** ~10 ms, all 100 orders delivered, zero data corruption

### Phase 2: Hitting the Wall — I/O Bottleneck
- Same 10 threads, but each order now simulates a **2-second GPS API call** (`Thread.sleep`)
- Exposes **thread starvation**: 10 threads sleeping = 0% CPU utilization, 90 orders stuck in queue
- **Result:** ~20 seconds for 100 orders (5 orders/sec throughput)

### Phase 3: JDK 21 — Virtual Thread Revolution
- Swaps to `Executors.newVirtualThreadPerTaskExecutor()`
- **10,000 orders** with the same 2-second sleep complete in **~2-3 seconds**
- Virtual threads **unmount** from carrier threads during `Thread.sleep`, allowing massive concurrency on a handful of OS threads

## 📊 Benchmark Comparison

| Metric | Phase 1 | Phase 2 | Phase 3 |
|---|---|---|---|
| Orders | 100 | 100 | 10,000 |
| Thread type | 10 platform | 10 platform | Virtual |
| I/O per order | None | 2,000 ms | 2,000 ms |
| Total time | **~10 ms** | **~20 sec** | **~2.5 sec** |
| Throughput | ~10,000/sec | 5/sec | ~4,000/sec |

## 🛠️ How to Build & Run

### Prerequisites
- **JDK 17+** (JDK 21+ for Phase 3)
- **Maven 3.8+**

### Using Maven (Recommended)

```bash
# Build the project
mvn clean compile

# Run Phase 1
mvn exec:java -Dexec.arguments="1"

# Run Phase 2
mvn exec:java -Dexec.arguments="2"

# Run both Phase 1 & 2 sequentially
mvn exec:java -Dexec.arguments="all"

# Run Phase 3 (when on JDK 21+)
mvn exec:java -Dexec.mainClass="runner.Phase3Runner"
```

### Or Compile & Run Manually

```bash
# Compile (Phase 1 & 2)
javac -encoding UTF-8 -d target/classes $(find src/main/java -name "*.java" ! -name "Phase3Runner.java")

# Run via Java CLI
java -cp target/classes Main 1
java -cp target/classes Main 2
```

## 🔑 Key Concurrency Concepts

### Why ConcurrentHashMap?
A regular `HashMap` is not thread-safe — concurrent `put()` calls can corrupt the internal bucket array. `Collections.synchronizedMap()` wraps every method in a single lock, serializing all access. `ConcurrentHashMap` uses **CAS + per-bin locking**, so threads writing to different keys never contend.

### What is Thread Starvation?
When all threads in a fixed pool are blocked on I/O (`Thread.sleep`, network calls, DB queries), no threads are available to process queued tasks — even though the CPU is idle.

### How Virtual Threads Solve It
When a virtual thread hits a blocking call, the JVM **unmounts** it from its carrier (OS) thread. The carrier immediately picks up the next virtual thread. Thousands of tasks can sleep concurrently on just a few OS threads.

## 🎙️ Interview Talking Points

> "I used a `ConcurrentHashMap` because a synchronized block on a standard `HashMap` would have bottlenecked my threads, defeating the purpose of the thread pool."

> "I simulated I/O-bound workloads and observed how a fixed thread pool led to thread starvation — 10 threads sleeping with 0% CPU usage."

> "I migrated to JDK 21 Virtual Threads, shifting from a resource-heavy OS context-switching model to a highly scalable JVM-managed scheduling model — going from 5 orders/sec to 4,000 orders/sec."

## 📄 License

This project is for educational and interview demonstration purposes.
