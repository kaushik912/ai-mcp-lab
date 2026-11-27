package com.aicommerce.storefront.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "customer_order", indexes = @Index(name = "idx_order_user", columnList = "userId"))
public class CustomerOrder {

    @Id
    private String orderId;

    /** Owner. Every query must be scoped by this column. */
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Instant orderDate;

    /** Promised delivery; basis for the late-delivery courtesy-coupon rule. */
    private Instant promisedDeliveryAt;

    /** Null until the order is actually delivered. */
    private Instant deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private String shipmentTrackingId;

    protected CustomerOrder() {}

    public CustomerOrder(String orderId, Long userId, Instant orderDate, Instant promisedDeliveryAt,
                         Instant deliveredAt, OrderStatus status, String shipmentTrackingId) {
        this.orderId = orderId;
        this.userId = userId;
        this.orderDate = orderDate;
        this.promisedDeliveryAt = promisedDeliveryAt;
        this.deliveredAt = deliveredAt;
        this.status = status;
        this.shipmentTrackingId = shipmentTrackingId;
    }

    public String getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public Instant getOrderDate() { return orderDate; }
    public Instant getPromisedDeliveryAt() { return promisedDeliveryAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public OrderStatus getStatus() { return status; }
    public String getShipmentTrackingId() { return shipmentTrackingId; }
}
