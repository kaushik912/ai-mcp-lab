package com.aicommerce.storefront.returns;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Read models handed to the model (and later the UI). No user ids inside. */
public final class OrderViews {

    private OrderViews() {}

    public record ItemView(String sku, String productName, String category, int quantity, BigDecimal pricePaid,
                           boolean boxOpened, String returnStatus, String rmaId, String voucherCode,
                           BigDecimal voucherValue) {}

    public record OrderView(String orderId, String status, Instant orderDate, Instant promisedDeliveryAt,
                            Instant deliveredAt, Long daysSinceDelivery, String shipmentTrackingId,
                            long hoursLate, boolean qualifiesForLateDeliveryCoupon, List<ItemView> items) {}

    public record ProfileView(String displayName, String loyaltyTier, String city, String pincode) {}
}
