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
 *  PHASE 2 — Hitting the Wall: Simulating I/O Bottlenecks
 * ═══════════════════════════════════════════════════════════
 *
 * Demonstrates:
 *   • Thread starvation under I/O-bound workloads
 *   • The cost of blocking OS threads with Thread.sleep()
 *   • Why a fixed thread pool is a poor fit for I/O-heavy tasks
 *
 * With 10 threads and a 2-second sleep per task:
 *   • Only 10 orders process every 2 seconds → 5 orders/sec
 *   • 100 orders take ~20 seconds (10 batches × 2 seconds)
 *   • CPU utilisation is near 0% — threads are SLEEPING, not computing
 *
 * This is "thread starvation": the thread pool is fully occupied
 * but doing zero useful work. The remaining 90 queued orders
 * are unnecessarily delayed.
 *
 * Run with: JDK 17+
 */
public class Phase2Runner {

    public static void main(String[] args) {
        System.out.println("========================================================");
        System.out.println("  PHASE 2: I/O Bottleneck — Thread Starvation Demo");
        System.out.println("========================================================");
        System.out.println();

        final int ORDER_COUNT = 100;
        final int THREAD_POOL_SIZE = 10;
        final long SIMULATED_LATENCY_MS = 2_000; // 2-second simulated GPS API call

        // ── Build the order batch ──
        List<DeliveryOrder> orders = new ArrayList<>();
        for (int i = 1; i <= ORDER_COUNT; i++) {
            orders.add(new DeliveryOrder("ORD-" + String.format("%04d", i), "Zone-" + (i % 5)));
        }

        // ── Initialize components ──
        OrderStateManager stateManager = new OrderStateManager();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        RouteDispatcher dispatcher = new RouteDispatcher(executor, stateManager, SIMULATED_LATENCY_MS);

        System.out.println("Dispatching " + ORDER_COUNT + " orders with " + THREAD_POOL_SIZE
                + " platform threads and " + SIMULATED_LATENCY_MS + "ms simulated I/O...");
        System.out.println("   (Expected time: ~" + (ORDER_COUNT / THREAD_POOL_SIZE * SIMULATED_LATENCY_MS / 1000) + " seconds)");
        System.out.println();

        // ── Dispatch & time ──
        long startTime = System.nanoTime();

        dispatcher.dispatchAll(orders);
        dispatcher.shutdown();

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        // ── Results ──
        System.out.println();
        System.out.println("----------------------- RESULTS ------------------------");
        System.out.printf("  Orders submitted  : %d%n", ORDER_COUNT);
        System.out.printf("  Thread pool size  : %d platform threads%n", THREAD_POOL_SIZE);
        System.out.printf("  Simulated I/O     : %d ms per order%n", SIMULATED_LATENCY_MS);
        System.out.printf("  Total time        : %d ms (%.1f seconds)%n", elapsedMs, elapsedMs / 1000.0);
        System.out.printf("  Throughput        : %.1f orders/sec%n", ORDER_COUNT / (elapsedMs / 1000.0));
        System.out.printf("  Orders DELIVERED  : %d%n", stateManager.countByStatus(DeliveryOrder.Status.DELIVERED));
        System.out.println("--------------------------------------------------------");

        System.out.println();
        System.out.println("ANALYSIS:");
        System.out.println("   * The CPU was nearly IDLE the entire time.");
        System.out.println("   * All 10 OS threads were SLEEPING (blocked on Thread.sleep).");
        System.out.println("   * 90 orders sat in the queue waiting for a thread to wake up.");
        System.out.println("   * This is THREAD STARVATION — threads are occupied but not working.");
        System.out.println("   -> Solution: Virtual Threads (Phase 3, JDK 21+)");
    }
}
