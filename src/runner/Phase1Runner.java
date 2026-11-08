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
 *  PHASE 1 — JDK 17: Fixed Thread Pool + Shared State
 * ═══════════════════════════════════════════════════════════
 *
 * Demonstrates:
 *   • ExecutorService with a FixedThreadPool of 10 platform (OS) threads
 *   • Safe concurrent writes to a ConcurrentHashMap
 *   • CPU-only workload (no simulated I/O latency)
 *
 * Expected behaviour:
 *   • All 100 orders complete very quickly (< 1 second)
 *   • All 100 orders reach DELIVERED status — no data corruption
 *   • The ConcurrentHashMap handles concurrent put() calls safely
 *
 * Run with: JDK 17+
 */
public class Phase1Runner {

    public static void main(String[] args) {
        System.out.println("========================================================");
        System.out.println("  PHASE 1: JDK 17 — Fixed Thread Pool + Shared State");
        System.out.println("========================================================");
        System.out.println();

        final int ORDER_COUNT = 100;
        final int THREAD_POOL_SIZE = 10;
        final long SIMULATED_LATENCY_MS = 0; // No I/O simulation in Phase 1

        // ── Build the order batch ──
        List<DeliveryOrder> orders = new ArrayList<>();
        for (int i = 1; i <= ORDER_COUNT; i++) {
            orders.add(new DeliveryOrder("ORD-" + String.format("%04d", i), "Zone-" + (i % 5)));
        }

        // ── Initialize components ──
        OrderStateManager stateManager = new OrderStateManager();

        /*
         * Executors.newFixedThreadPool(10):
         *   Creates a pool of exactly 10 OS (platform) threads.
         *   Each thread has a ~1 MB native stack allocated by the OS.
         *   Tasks beyond the first 10 wait in an unbounded LinkedBlockingQueue.
         */
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        RouteDispatcher dispatcher = new RouteDispatcher(executor, stateManager, SIMULATED_LATENCY_MS);

        // ── Dispatch & time ──
        long startTime = System.nanoTime();

        dispatcher.dispatchAll(orders);
        dispatcher.shutdown();

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        // ── Results ──
        System.out.println("----------------------- RESULTS ------------------------");
        System.out.printf("  Orders submitted  : %d%n", ORDER_COUNT);
        System.out.printf("  Thread pool size  : %d platform threads%n", THREAD_POOL_SIZE);
        System.out.printf("  Total time        : %d ms%n", elapsedMs);
        System.out.printf("  Orders DELIVERED  : %d%n", stateManager.countByStatus(DeliveryOrder.Status.DELIVERED));
        System.out.printf("  Orders IN_TRANSIT : %d%n", stateManager.countByStatus(DeliveryOrder.Status.IN_TRANSIT));
        System.out.printf("  Orders PENDING    : %d%n", stateManager.countByStatus(DeliveryOrder.Status.PENDING));
        System.out.println("--------------------------------------------------------");

        if (stateManager.countByStatus(DeliveryOrder.Status.DELIVERED) == ORDER_COUNT) {
            System.out.println("SUCCESS: All " + ORDER_COUNT + " orders safely delivered. No data corruption!");
        } else {
            System.out.println("FAILURE: Some orders were lost or stuck. Investigate thread safety.");
        }
    }
}
