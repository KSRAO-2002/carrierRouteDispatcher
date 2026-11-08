package runner;

import dispatcher.RouteDispatcher;
import model.DeliveryOrder;
import state.OrderStateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ═══════════════════════════════════════════════════════════
 *  PHASE 3 — JDK 21: Virtual Thread Revolution
 * ═══════════════════════════════════════════════════════════
 *
 * ⚠ REQUIRES JDK 21+ TO COMPILE AND RUN.
 *
 * Demonstrates:
 *   • Virtual Threads via Executors.newVirtualThreadPerTaskExecutor()
 *   • Massive I/O concurrency without thread starvation
 *   • The JVM's ability to unmount/remount virtual threads from carrier threads
 *
 * How it works:
 *   1. Each submitted task gets its OWN virtual thread (not pooled).
 *   2. Virtual threads are lightweight (~few KB of stack vs ~1 MB for platform threads).
 *   3. When a virtual thread hits Thread.sleep() or any blocking I/O:
 *        a. The JVM UNMOUNTS it from the carrier (OS) thread.
 *        b. The carrier thread immediately picks up another virtual thread.
 *        c. When the sleep/I/O completes, the virtual thread is REMOUNTED
 *           onto any available carrier thread.
 *   4. Result: 10,000 tasks sleep concurrently, but only ~CPU-core-count
 *      carrier threads are actually used by the OS.
 *
 * Expected behaviour:
 *   • 10,000 orders with 2-second sleep complete in ~2-3 seconds total
 *   • Compare to Phase 2: 100 orders took ~20 seconds!
 *   • That's a 500x improvement in throughput for I/O-bound workloads
 *
 * Run with: JDK 21+
 */
public class Phase3Runner {

    public static void main(String[] args) {
        System.out.println("========================================================");
        System.out.println("  PHASE 3: JDK 21 — Virtual Thread Revolution");
        System.out.println("========================================================");
        System.out.println();

        final int ORDER_COUNT = 10_000;
        final long SIMULATED_LATENCY_MS = 2_000; // Same 2-second I/O simulation as Phase 2

        // ── Build the order batch ──
        List<DeliveryOrder> orders = new ArrayList<>();
        for (int i = 1; i <= ORDER_COUNT; i++) {
            orders.add(new DeliveryOrder("ORD-" + String.format("%05d", i), "Zone-" + (i % 20)));
        }

        // ── Initialize components ──
        OrderStateManager stateManager = new OrderStateManager();

        /*
         * Executors.newVirtualThreadPerTaskExecutor() (JDK 21+):
         *
         * Creates a NEW virtual thread for EVERY submitted task.
         * Unlike a FixedThreadPool, there is NO queue — every task starts immediately.
         *
         * Virtual threads are scheduled by the JVM onto a small pool of
         * "carrier" platform threads (typically = number of CPU cores).
         *
         * Key difference:
         *   Platform thread: 1 thread = 1 OS thread = ~1 MB stack = expensive
         *   Virtual thread:  1 thread = JVM-managed = ~few KB = practically free
         */
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        RouteDispatcher dispatcher = new RouteDispatcher(executor, stateManager, SIMULATED_LATENCY_MS);

        System.out.println("Dispatching " + ORDER_COUNT + " orders with VIRTUAL THREADS");
        System.out.println("   and " + SIMULATED_LATENCY_MS + "ms simulated I/O per order...");
        System.out.println();

        // ── Dispatch & time ──
        long startTime = System.nanoTime();

        dispatcher.dispatchAll(orders);
        dispatcher.shutdown();

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        // ── Results ──
        System.out.println();
        System.out.println("----------------------- RESULTS ------------------------");
        System.out.printf("  Orders submitted  : %,d%n", ORDER_COUNT);
        System.out.printf("  Thread type       : Virtual Threads (JDK 21)%n");
        System.out.printf("  Simulated I/O     : %d ms per order%n", SIMULATED_LATENCY_MS);
        System.out.printf("  Total time        : %,d ms (%.1f seconds)%n", elapsedMs, elapsedMs / 1000.0);
        System.out.printf("  Throughput        : %,.0f orders/sec%n", ORDER_COUNT / (elapsedMs / 1000.0));
        System.out.printf("  Orders DELIVERED  : %,d%n", stateManager.countByStatus(DeliveryOrder.Status.DELIVERED));
        System.out.println("--------------------------------------------------------");

        System.out.println();
        System.out.println("COMPARISON WITH PHASE 2:");
        System.out.println("   Phase 2: 100  orders, 10 platform threads, ~20 seconds  (5 orders/sec)");
        System.out.printf("   Phase 3: %,d orders, virtual threads,   ~%.0f seconds  (%,.0f orders/sec)%n",
                ORDER_COUNT, elapsedMs / 1000.0, ORDER_COUNT / (elapsedMs / 1000.0));
        System.out.println();
        System.out.println("WHY IT'S FAST:");
        System.out.println("   * When a virtual thread hits Thread.sleep(), the JVM UNMOUNTS it.");
        System.out.println("   * The OS carrier thread immediately picks up the next virtual thread.");
        System.out.println("   * All " + ORDER_COUNT + " tasks sleep CONCURRENTLY on just a few OS threads.");
        System.out.println("   * The CPU is never idle — it's always scheduling the next ready task.");
    }
}
