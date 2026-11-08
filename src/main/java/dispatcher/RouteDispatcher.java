package dispatcher;

import model.DeliveryOrder;
import state.OrderStateManager;
import task.DeliveryTask;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates order processing by submitting DeliveryTasks
 * to a configurable ExecutorService.
 *
 * The dispatcher is decoupled from the thread pool implementation —
 * it accepts any ExecutorService, making it trivial to swap between:
 *   • Executors.newFixedThreadPool(10)              → Phase 1 & 2 (platform threads)
 *   • Executors.newVirtualThreadPerTaskExecutor()    → Phase 3 (virtual threads, JDK 21+)
 */
public class RouteDispatcher {

    private final ExecutorService executor;
    private final OrderStateManager stateManager;
    private final long simulatedLatencyMs;

    public RouteDispatcher(ExecutorService executor, OrderStateManager stateManager, long simulatedLatencyMs) {
        this.executor = executor;
        this.stateManager = stateManager;
        this.simulatedLatencyMs = simulatedLatencyMs;
    }

    /**
     * Dispatches a batch of orders for concurrent processing.
     *
     * Each order is:
     *   1. Registered with PENDING status in the shared state.
     *   2. Wrapped in a DeliveryTask Runnable.
     *   3. Submitted to the ExecutorService's work queue.
     *
     * With a FixedThreadPool(10), at most 10 tasks run concurrently — the rest queue.
     * With a VirtualThreadPerTaskExecutor, ALL tasks can run concurrently.
     */
    public void dispatchAll(List<DeliveryOrder> orders) {
        for (DeliveryOrder order : orders) {
            stateManager.registerOrder(order);
            executor.submit(new DeliveryTask(order, stateManager, simulatedLatencyMs));
        }
    }

    /**
     * Initiates orderly shutdown: no new tasks accepted,
     * waits for in-flight tasks to complete.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                System.err.println("WARNING: Executor did not terminate within timeout — forcing shutdown.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
