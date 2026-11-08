package model;

/**
 * Immutable data model representing a delivery order.
 * Using a record (JDK 16+) for conciseness — auto-generates
 * constructor, getters, equals(), hashCode(), and toString().
 */
public record DeliveryOrder(String orderId, String destination) {

    /**
     * Possible statuses an order transitions through.
     * Using an enum prevents typos and makes the state machine explicit.
     */
    public enum Status {
        PENDING,
        IN_TRANSIT,
        DELIVERED
    }
}
