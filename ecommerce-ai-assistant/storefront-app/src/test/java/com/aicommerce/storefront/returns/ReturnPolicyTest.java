package com.aicommerce.storefront.returns;

import static com.aicommerce.storefront.returns.DecisionStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.aicommerce.storefront.domain.Category;
import com.aicommerce.storefront.domain.LoyaltyTier;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/** Pure policy rules at exact day boundaries. No Spring, no LLM. */
class ReturnPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-27T12:00:00Z");

    private static Instant daysAgo(int d) {
        return NOW.minus(Duration.ofDays(d));
    }

    @ParameterizedTest(name = "{0} day {1} opened={2} defective={3} -> {4}")
    @CsvSource({
            // Apparel / footwear: 30-day refund, 31-45 exchange credit, then nothing
            "FOOTWEAR,    4,  false, false, APPROVED",
            "APPAREL,     30, false, false, APPROVED",
            "APPAREL,     31, false, false, ELIGIBLE_FOR_EXCHANGE_CREDIT",
            "APPAREL,     36, false, false, ELIGIBLE_FOR_EXCHANGE_CREDIT",
            "APPAREL,     45, false, false, ELIGIBLE_FOR_EXCHANGE_CREDIT",
            "APPAREL,     46, false, false, REJECTED_OUTSIDE_WINDOW",
            // Electronics: 14-day hard stop, no grace
            "ELECTRONICS, 14, false, false, APPROVED",
            "ELECTRONICS, 15, false, false, REJECTED_OUTSIDE_WINDOW",
            "ELECTRONICS, 15, true,  true,  REJECTED_OUTSIDE_WINDOW",
            // Hygiene: opened = final sale even if 'defective' (warranty instead); unopened 14 days
            "HYGIENE,     2,  true,  false, REJECTED_NON_RETURNABLE",
            "HYGIENE,     2,  true,  true,  REJECTED_NON_RETURNABLE",
            "HYGIENE,     14, false, false, APPROVED",
            "HYGIENE,     15, false, false, REJECTED_OUTSIDE_WINDOW",
    })
    void returnWindows(Category cat, int days, boolean opened, boolean defective, DecisionStatus expected) {
        assertThat(ReturnPolicy.evaluateReturn(cat, "ANY-SKU", opened, defective, daysAgo(days), NOW).status())
                .isEqualTo(expected);
    }

    @Test
    void openedEarbudsOnlyReturnableIfDefective() {
        assertThat(ReturnPolicy.evaluateReturn(Category.ELECTRONICS, "AERO-BUDS-01", true, false, daysAgo(6), NOW))
                .extracting(ReturnPolicy.Verdict::status, ReturnPolicy.Verdict::policyRef)
                .containsExactly(REJECTED_NON_RETURNABLE, "POL-3");
        assertThat(ReturnPolicy.evaluateReturn(Category.ELECTRONICS, "AERO-BUDS-01", true, true, daysAgo(6), NOW).status())
                .isEqualTo(APPROVED);
        // Opened non-audio electronics (power bank) are fine within 14 days
        assertThat(ReturnPolicy.evaluateReturn(Category.ELECTRONICS, "VOLT-BANK-01", true, false, daysAgo(6), NOW).status())
                .isEqualTo(APPROVED);
    }

    @Test
    void dayCountUsesWholeDays() {
        // 30 days 23 hours is still day 30
        Instant delivered = NOW.minus(Duration.ofDays(30).plusHours(23));
        assertThat(ReturnPolicy.evaluateReturn(Category.APPAREL, "X", false, false, delivered, NOW).status())
                .isEqualTo(APPROVED);
    }

    @Test
    void notDeliveredCannotBeReturned() {
        assertThat(ReturnPolicy.evaluateReturn(Category.APPAREL, "X", false, false, null, NOW).status())
                .isEqualTo(NOT_DELIVERED);
    }

    @Test
    void voucherOnlyInGraceWindowForApparelAndFootwear() {
        assertThat(ReturnPolicy.evaluateVoucher(Category.APPAREL, daysAgo(36), NOW).status()).isEqualTo(ELIGIBLE_FOR_EXCHANGE_CREDIT);
        assertThat(ReturnPolicy.evaluateVoucher(Category.APPAREL, daysAgo(20), NOW).status()).isEqualTo(NOT_ELIGIBLE_FOR_VOUCHER);
        assertThat(ReturnPolicy.evaluateVoucher(Category.APPAREL, daysAgo(46), NOW).status()).isEqualTo(REJECTED_OUTSIDE_WINDOW);
        assertThat(ReturnPolicy.evaluateVoucher(Category.ELECTRONICS, daysAgo(36), NOW).status()).isEqualTo(NOT_ELIGIBLE_FOR_VOUCHER);
    }

    @Test
    void voucherValueByTier() {
        assertThat(ReturnPolicy.voucherValue(new BigDecimal("12999"), 1, ReturnPolicy.creditPercent(LoyaltyTier.VIP)))
                .isEqualByComparingTo("11699.10");
        assertThat(ReturnPolicy.voucherValue(new BigDecimal("12999"), 1, ReturnPolicy.creditPercent(LoyaltyTier.STANDARD)))
                .isEqualByComparingTo("10399.20");
    }

    @Test
    void lateCouponNeedsMoreThan24Hours() {
        Instant promised = NOW.minus(Duration.ofHours(24));
        assertThat(ReturnPolicy.qualifiesForLateCoupon(ReturnPolicy.lateness(promised, null, NOW))).isFalse();
        assertThat(ReturnPolicy.qualifiesForLateCoupon(ReturnPolicy.lateness(promised.minusSeconds(60), null, NOW))).isTrue();
        // Delivered on time
        assertThat(ReturnPolicy.lateness(NOW, NOW.minusSeconds(10), NOW)).isZero();
    }
}
