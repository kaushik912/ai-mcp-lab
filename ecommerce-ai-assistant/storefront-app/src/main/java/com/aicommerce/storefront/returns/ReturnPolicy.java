package com.aicommerce.storefront.returns;

import com.aicommerce.storefront.domain.Category;
import com.aicommerce.storefront.domain.LoyaltyTier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * The deterministic mirror of docs/return-and-warranty-policy.md. Pure functions, no I/O.
 * Day count = whole days between deliveredAt and now (a parcel delivered 30d 23h ago is on day 30).
 */
public final class ReturnPolicy {

    public static final int APPAREL_FOOTWEAR_REFUND_DAYS = 30;  // POL-1
    public static final int ELECTRONICS_DAYS = 14;              // POL-2
    public static final int HYGIENE_UNOPENED_DAYS = 14;         // POL-5
    public static final int GRACE_LAST_DAY = 45;                // POL-4
    public static final int STANDARD_CREDIT_PERCENT = 80;       // POL-4
    public static final int VIP_CREDIT_PERCENT = 90;            // POL-4
    public static final Duration LATE_DELIVERY_THRESHOLD = Duration.ofHours(24); // POL-7

    /** In-ear audio covered by the hygiene/seal rule (POL-3). The catalog has no sub-category, so it's an explicit list. */
    static final Set<String> IN_EAR_AUDIO_SKUS = Set.of("AERO-BUDS-01");

    private ReturnPolicy() {}

    public record Verdict(DecisionStatus status, long days, String policyRef, String message) {}

    public static long daysSince(Instant deliveredAt, Instant now) {
        return Duration.between(deliveredAt, now).toDays();
    }

    public static Verdict evaluateReturn(Category category, String sku, boolean boxOpened, boolean defective,
                                         Instant deliveredAt, Instant now) {
        if (deliveredAt == null) {
            return new Verdict(DecisionStatus.NOT_DELIVERED, -1, "POL-8", "The order has not been delivered yet.");
        }
        long days = daysSince(deliveredAt, now);
        return switch (category) {
            case APPAREL, FOOTWEAR -> {
                if (days <= APPAREL_FOOTWEAR_REFUND_DAYS) {
                    yield new Verdict(DecisionStatus.APPROVED, days, "POL-1",
                            "Delivered " + days + " days ago, within the 30-day window. Full refund once the unworn item with tags is received.");
                }
                if (days <= GRACE_LAST_DAY) {
                    yield new Verdict(DecisionStatus.ELIGIBLE_FOR_EXCHANGE_CREDIT, days, "POL-4",
                            "Delivered " + days + " days ago: past the 30-day refund window, so no refund to the original payment method. "
                                    + "Eligible for an exchange / store-credit voucher (31-45 day grace policy).");
                }
                yield new Verdict(DecisionStatus.REJECTED_OUTSIDE_WINDOW, days, "POL-4",
                        "Delivered " + days + " days ago: past both the 30-day refund window and the 45-day grace period.");
            }
            case ELECTRONICS -> {
                if (days > ELECTRONICS_DAYS) {
                    yield new Verdict(DecisionStatus.REJECTED_OUTSIDE_WINDOW, days, "POL-2",
                            "Delivered " + days + " days ago: past the strict 14-day electronics window. Defects are covered by the 1-year warranty (POL-6).");
                }
                if (IN_EAR_AUDIO_SKUS.contains(sku) && boxOpened && !defective) {
                    yield new Verdict(DecisionStatus.REJECTED_NON_RETURNABLE, days, "POL-3",
                            "Opened in-ear audio can only be returned if defective (hygiene & seal rule). The item is opened and was not reported defective.");
                }
                String ref = IN_EAR_AUDIO_SKUS.contains(sku) && boxOpened ? "POL-3" : "POL-2";
                yield new Verdict(DecisionStatus.APPROVED, days, ref,
                        "Delivered " + days + " days ago, within the 14-day electronics window"
                                + (defective ? "; reported defective, so it will be inspected on receipt." : "."));
            }
            case HYGIENE -> {
                if (boxOpened) {
                    yield new Verdict(DecisionStatus.REJECTED_NON_RETURNABLE, days, "POL-5",
                            "Opened personal-care items are final sale. If it was dead on arrival, a warranty claim applies (POL-6).");
                }
                if (days > HYGIENE_UNOPENED_DAYS) {
                    yield new Verdict(DecisionStatus.REJECTED_OUTSIDE_WINDOW, days, "POL-5",
                            "Delivered " + days + " days ago: unopened hygiene items must be returned within 14 days.");
                }
                yield new Verdict(DecisionStatus.APPROVED, days, "POL-5",
                        "Unopened hygiene item within 14 days: full refund.");
            }
        };
    }

    /** Voucher eligibility (POL-4). Returns APPROVED-like status ELIGIBLE_FOR_EXCHANGE_CREDIT when a voucher may be issued. */
    public static Verdict evaluateVoucher(Category category, Instant deliveredAt, Instant now) {
        if (deliveredAt == null) {
            return new Verdict(DecisionStatus.NOT_DELIVERED, -1, "POL-8", "The order has not been delivered yet.");
        }
        long days = daysSince(deliveredAt, now);
        if (category != Category.APPAREL && category != Category.FOOTWEAR) {
            return new Verdict(DecisionStatus.NOT_ELIGIBLE_FOR_VOUCHER, days, "POL-4",
                    "The 31-45 day exchange voucher applies only to apparel and footwear.");
        }
        if (days <= APPAREL_FOOTWEAR_REFUND_DAYS) {
            return new Verdict(DecisionStatus.NOT_ELIGIBLE_FOR_VOUCHER, days, "POL-1",
                    "Delivered " + days + " days ago: still inside the 30-day window, so a full refund is available instead. Use a normal return.");
        }
        if (days > GRACE_LAST_DAY) {
            return new Verdict(DecisionStatus.REJECTED_OUTSIDE_WINDOW, days, "POL-4",
                    "Delivered " + days + " days ago: past the 45-day grace period.");
        }
        return new Verdict(DecisionStatus.ELIGIBLE_FOR_EXCHANGE_CREDIT, days, "POL-4",
                "Delivered " + days + " days ago: inside the 31-45 day grace period.");
    }

    public static int creditPercent(LoyaltyTier tier) {
        return tier == LoyaltyTier.VIP ? VIP_CREDIT_PERCENT : STANDARD_CREDIT_PERCENT;
    }

    public static BigDecimal voucherValue(BigDecimal pricePaid, int quantity, int percent) {
        return pricePaid.multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** How late the order is versus the promise: delivered late, or still undelivered past the promise. Zero if on time. */
    public static Duration lateness(Instant promisedAt, Instant deliveredAt, Instant now) {
        if (promisedAt == null) {
            return Duration.ZERO;
        }
        Instant reference = deliveredAt != null ? deliveredAt : now;
        Duration d = Duration.between(promisedAt, reference);
        return d.isNegative() ? Duration.ZERO : d;
    }

    public static boolean qualifiesForLateCoupon(Duration lateness) {
        return lateness.compareTo(LATE_DELIVERY_THRESHOLD) > 0;
    }
}
