package com.aicommerce.storefront.returns;

import com.aicommerce.storefront.domain.AppUser;
import com.aicommerce.storefront.domain.CustomerOrder;
import com.aicommerce.storefront.domain.OrderItem;
import com.aicommerce.storefront.domain.ReturnStatus;
import com.aicommerce.storefront.repo.AppUserRepository;
import com.aicommerce.storefront.repo.CustomerOrderRepository;
import com.aicommerce.storefront.repo.OrderItemRepository;
import com.aicommerce.storefront.repo.ProductRepository;
import com.aicommerce.storefront.returns.OrderViews.ItemView;
import com.aicommerce.storefront.returns.OrderViews.OrderView;
import com.aicommerce.storefront.returns.OrderViews.ProfileView;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every method takes the authenticated user's id as its first argument. Callers get it from the
 * server (ToolContext / SecurityContext), never from the model. Every lookup is owner-scoped, and
 * another customer's order looks exactly like a missing one (NOT_FOUND), so existence never leaks.
 */
@Service
public class ReturnService {

    private static final Logger log = LoggerFactory.getLogger(ReturnService.class);

    private final AppUserRepository users;
    private final CustomerOrderRepository orders;
    private final OrderItemRepository items;
    private final ProductRepository products;
    private final Clock clock;

    public ReturnService(AppUserRepository users, CustomerOrderRepository orders, OrderItemRepository items,
                         ProductRepository products, Clock clock) {
        this.users = users;
        this.orders = orders;
        this.items = items;
        this.products = products;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProfileView profile(Long userId) {
        AppUser u = users.findById(userId).orElseThrow();
        return new ProfileView(u.getDisplayName(), u.getLoyaltyTier().name(), u.getCity(), u.getPincode());
    }

    @Transactional(readOnly = true)
    public List<OrderView> myOrders(Long userId) {
        Instant now = clock.instant();
        return orders.findAllForUser(userId).stream().map(o -> {
            Duration late = ReturnPolicy.lateness(o.getPromisedDeliveryAt(), o.getDeliveredAt(), now);
            List<ItemView> itemViews = items.findByOrderForUser(o.getOrderId(), userId).stream().map(i ->
                    new ItemView(i.getSku(), i.getProductName(), i.getCategory().name(), i.getQuantity(),
                            i.getPricePaid(), i.isBoxOpened(), i.getReturnStatus().name(), i.getRmaId(),
                            i.getVoucherCode(), i.getVoucherValue())).toList();
            return new OrderView(o.getOrderId(), o.getStatus().name(), o.getOrderDate(), o.getPromisedDeliveryAt(),
                    o.getDeliveredAt(),
                    o.getDeliveredAt() == null ? null : ReturnPolicy.daysSince(o.getDeliveredAt(), now),
                    o.getShipmentTrackingId(), late.toHours(), ReturnPolicy.qualifiesForLateCoupon(late), itemViews);
        }).toList();
    }

    @Transactional
    public ReturnDecision initiateReturn(Long userId, String orderId, String skuOrName, String reason, boolean defective) {
        Optional<CustomerOrder> order = orders.findByIdForUser(normalise(orderId), userId);
        Optional<OrderItem> found = order.flatMap(o -> lockItem(o, skuOrName, userId));
        if (found.isEmpty()) {
            log.info("initiateReturn NOT_FOUND user={} order={}", userId, orderId);
            return ReturnDecision.notFound(orderId, skuOrName);
        }
        CustomerOrder o = order.get();
        OrderItem item = found.get();

        // Idempotency: the item row is the single source of truth, read under a row lock.
        if (item.getReturnStatus() == ReturnStatus.RETURN_APPROVED) {
            return decision(DecisionStatus.APPROVED, o, item, item.getRmaId(), null, null, null, "POL-1",
                    "A return was already approved for this item. Same RMA returned; no duplicate created.",
                    nextStepForApproved(), true);
        }
        if (item.getReturnStatus() == ReturnStatus.EXCHANGE_ISSUED) {
            return decision(DecisionStatus.REJECTED_NON_RETURNABLE, o, item, null, item.getVoucherCode(),
                    item.getVoucherValue(), null, "POL-4",
                    "An exchange voucher was already issued for this item, so it can no longer be returned.",
                    "Remind the customer of their existing voucher code.", true);
        }

        ReturnPolicy.Verdict v = ReturnPolicy.evaluateReturn(item.getCategory(), item.getSku(), item.isBoxOpened(),
                defective, o.getDeliveredAt(), clock.instant());
        log.info("initiateReturn user={} order={} sku={} days={} -> {}", userId, o.getOrderId(), item.getSku(),
                v.days(), v.status());

        return switch (v.status()) {
            case APPROVED -> {
                String rma = "RMA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                item.setRmaId(rma);
                item.setReturnStatus(ReturnStatus.RETURN_APPROVED);
                // Demo choice (user-confirmed): restock on approval so the catalog's live stock visibly changes.
                // Only on a NEW approval; the idempotent replay above returns early, so stock never double-counts.
                products.findById(item.getSku())
                        .ifPresent(p -> p.setStockCount(p.getStockCount() + item.getQuantity()));
                yield decision(v.status(), o, item, rma, null, null, null, v.policyRef(),
                        v.message() + (reason == null || reason.isBlank() ? "" : " Reason noted: " + reason + "."),
                        nextStepForApproved(), false);
            }
            case ELIGIBLE_FOR_EXCHANGE_CREDIT -> {
                int pct = ReturnPolicy.creditPercent(tierOf(userId));
                BigDecimal value = ReturnPolicy.voucherValue(item.getPricePaid(), item.getQuantity(), pct);
                yield decision(v.status(), o, item, null, null, value, pct, v.policyRef(), v.message(),
                        "Refund is NOT possible. Proactively offer an exchange/store-credit voucher worth " + pct
                                + "% (₹" + value + "). Call issueExchangeVoucher only after the customer agrees.",
                        false);
            }
            default -> decision(v.status(), o, item, null, null, null, null, v.policyRef(), v.message(),
                    "Explain the decision and the policy id. Do not promise a return.", false);
        };
    }

    @Transactional
    public ReturnDecision issueExchangeVoucher(Long userId, String orderId, String skuOrName) {
        Optional<CustomerOrder> order = orders.findByIdForUser(normalise(orderId), userId);
        Optional<OrderItem> found = order.flatMap(o -> lockItem(o, skuOrName, userId));
        if (found.isEmpty()) {
            return ReturnDecision.notFound(orderId, skuOrName);
        }
        CustomerOrder o = order.get();
        OrderItem item = found.get();

        if (item.getReturnStatus() == ReturnStatus.EXCHANGE_ISSUED) {
            return decision(DecisionStatus.VOUCHER_ISSUED, o, item, null, item.getVoucherCode(), item.getVoucherValue(),
                    null, "POL-4", "Voucher was already issued for this item. Same code returned; no duplicate created.",
                    "Share the voucher code and value.", true);
        }
        if (item.getReturnStatus() == ReturnStatus.RETURN_APPROVED) {
            return decision(DecisionStatus.NOT_ELIGIBLE_FOR_VOUCHER, o, item, item.getRmaId(), null, null, null,
                    "POL-4", "A refund return is already approved for this item, so no voucher can be issued.",
                    "Explain that the item is already being refunded.", true);
        }

        ReturnPolicy.Verdict v = ReturnPolicy.evaluateVoucher(item.getCategory(), o.getDeliveredAt(), clock.instant());
        log.info("issueExchangeVoucher user={} order={} sku={} days={} -> {}", userId, o.getOrderId(), item.getSku(),
                v.days(), v.status());
        if (v.status() != DecisionStatus.ELIGIBLE_FOR_EXCHANGE_CREDIT) {
            return decision(v.status(), o, item, null, null, null, null, v.policyRef(), v.message(),
                    "Explain why no voucher can be issued.", false);
        }
        int pct = ReturnPolicy.creditPercent(tierOf(userId));
        BigDecimal value = ReturnPolicy.voucherValue(item.getPricePaid(), item.getQuantity(), pct);
        String code = "VCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        item.issueVoucher(code, value);
        return decision(DecisionStatus.VOUCHER_ISSUED, o, item, null, code, value, pct, v.policyRef(),
                v.message() + " Voucher worth " + pct + "% of the price paid, valid 180 days.",
                "Share the voucher code and value with the customer.", false);
    }

    /**
     * The model may pass a SKU, a product name, or nothing. Resolve within this (already owner-scoped) order,
     * then re-read the row under a lock.
     */
    private Optional<OrderItem> lockItem(CustomerOrder order, String skuOrName, Long userId) {
        List<OrderItem> candidates = items.findByOrderForUser(order.getOrderId(), userId);
        Optional<OrderItem> match = Optional.empty();
        if (skuOrName != null && !skuOrName.isBlank()) {
            String q = skuOrName.trim().toLowerCase();
            match = candidates.stream()
                    .filter(i -> i.getSku().equalsIgnoreCase(q) || i.getProductName().toLowerCase().contains(q))
                    .findFirst();
        }
        if (match.isEmpty() && candidates.size() == 1) {
            match = Optional.of(candidates.get(0));
        }
        return match.flatMap(i -> items.lockByOrderAndSkuForUser(order.getOrderId(), i.getSku(), userId));
    }

    private com.aicommerce.storefront.domain.LoyaltyTier tierOf(Long userId) {
        return users.findById(userId).orElseThrow().getLoyaltyTier();
    }

    private static String normalise(String orderId) {
        return orderId == null ? "" : orderId.trim().toUpperCase();
    }

    private static String nextStepForApproved() {
        return "Share the RMA id. Then generate a prepaid return label with the carrier's generateReturnShippingLabel tool "
                + "(use this rmaId, the customer's display name and pincode from getMyProfile, and this item's category), "
                + "and offer nearby drop-off points.";
    }

    private ReturnDecision decision(DecisionStatus status, CustomerOrder o, OrderItem i, String rma, String voucher,
                                    BigDecimal voucherValue, Integer pct, String policyRef, String message,
                                    String nextStep, boolean replay) {
        Long days = o.getDeliveredAt() == null ? null : ReturnPolicy.daysSince(o.getDeliveredAt(), clock.instant());
        return new ReturnDecision(status, o.getOrderId(), i.getSku(), i.getProductName(), i.getCategory().name(),
                days, rma, voucher, voucherValue, pct, policyRef, message, nextStep, replay);
    }
}
