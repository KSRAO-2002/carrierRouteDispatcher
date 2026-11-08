package task;

import model.DeliveryOrder;
import state.OrderStateManager;

/**
 * The unit of work submitted to the thread pool.
 *
 * Each DeliveryTask transitions a single order through its lifecycle:
 *   PENDING  →  IN_TRANSIT  →  (simulate work / I/O)  →  DELIVERED
 *
 * Implements Runnable so it can be submitted to any ExecutorService.
 */
public class DeliveryTask implements Runnable {

    private final DeliveryOrder order;
    private final OrderStateManager stateManager;
    private final long simulatedLatencyMs;

    /**
     * @param order              The order to process.
     * @param stateManager       Shared state holding all order statuses.
     * @param simulatedLatencyMs Milliseconds to sleep (simulates API/DB call).
     *                           Set to 0 for Phase 1 (CPU-only), 2000 for Phase 2/3 (I/O).
     */
    public DeliveryTask(DeliveryOrder order, OrderStateManager stateManager, long simulatedLatencyMs) {
        this.order = order;
        this.stateManager = stateManager;
        this.simulatedLatencyMs = simulatedLatencyMs;
    }

    @Override
    public void run() {
        // ── Step 1: Mark order as IN_TRANSIT ──
        stateManager.updateStatus(order.orderId(), DeliveryOrder.Status.IN_TRANSIT);

        // ── Step 2: Simulate I/O latency (e.g., GPS tracking API call) ──
        if (simulatedLatencyMs > 0) {
            try {
                /*
                 * Thread.sleep() is a BLOCKING call.
                 *
                 * Platform threads (JDK 17 FixedThreadPool):
                 *   The OS thread is parked. It holds its ~1 MB stack and OS scheduling slot
                 *   but does ZERO useful work. With 10 threads and 2s sleep, only 5 orders/sec.
                 *
                 * Virtual threads (JDK 21):
                 *   The JVM detects the blocking call, UNMOUNTS the virtual thread from its
                 *   carrier (OS) thread, and immediately schedules another virtual thread.
                 *   The carrier thread is never idle. 10,000 orders sleep concurrently on
                 *   just a handful of carrier threads.
                 */
                Thread.sleep(simulatedLatencyMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        // ── Step 3: Simulate CPU-bound computation (e.g., route optimisation) ──
        simulateCpuWork();

        // ── Step 4: Mark order as DELIVERED ──
        stateManager.updateStatus(order.orderId(), DeliveryOrder.Status.DELIVERED);
    }

    /**
     * Performs a small loop of basic math to simulate CPU work.
     * This keeps the thread genuinely busy (not sleeping).
     */
    private void simulateCpuWork() {
        double result = 0;
        for (int i = 0; i < 1_000; i++) {
            result += Math.sin(i) * Math.cos(i);
        }
        // Prevent JIT from optimizing away the loop
        if (result == Double.MAX_VALUE) {
            System.out.println("Unreachable");
        }
    }
}
