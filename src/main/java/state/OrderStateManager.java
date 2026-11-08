package state;

import model.DeliveryOrder;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe shared state for order statuses.
 *
 * WHY ConcurrentHashMap?
 * ──────────────────────
 * A regular HashMap is NOT thread-safe. If two threads call put()
 * simultaneously, the internal bucket array can corrupt — entries
 * silently vanish or the map enters an infinite loop during resize.
 *
 * Alternatives considered:
 *
 *   Collections.synchronizedMap(new HashMap<>())
 *       → Wraps every method in a synchronized block on a SINGLE monitor.
 *       → Every thread contends for the same lock, serialising all access.
 *       → Under 10+ threads, this becomes a severe bottleneck.
 *
 *   ConcurrentHashMap (chosen)
 *       → Uses fine-grained, lock-striped synchronisation (JDK 8+: CAS + synchronized on individual bins).
 *       → Multiple threads can read AND write concurrently to different buckets with zero contention.
 *       → Perfect for a high-throughput dispatcher where thousands of orders update in parallel.
 */
public class OrderStateManager {

    private final ConcurrentHashMap<String, DeliveryOrder.Status> orderStatuses = new ConcurrentHashMap<>();

    /**
     * Registers a new order with PENDING status.
     */
    public void registerOrder(DeliveryOrder order) {
        orderStatuses.put(order.orderId(), DeliveryOrder.Status.PENDING);
    }

    /**
     * Updates the status of an existing order.
     * ConcurrentHashMap.put() is atomic for single key operations.
     */
    public void updateStatus(String orderId, DeliveryOrder.Status status) {
        orderStatuses.put(orderId, status);
    }

    /**
     * Returns the current status of an order.
     */
    public DeliveryOrder.Status getStatus(String orderId) {
        return orderStatuses.get(orderId);
    }

    /**
     * Returns an unmodifiable snapshot of all order statuses.
     */
    public Map<String, DeliveryOrder.Status> getAllStatuses() {
        return Collections.unmodifiableMap(orderStatuses);
    }

    /**
     * Returns the total number of tracked orders.
     */
    public int size() {
        return orderStatuses.size();
    }

    /**
     * Counts how many orders have a given status.
     */
    public long countByStatus(DeliveryOrder.Status status) {
        return orderStatuses.values().stream()
                .filter(s -> s == status)
                .count();
    }
}
