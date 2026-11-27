package com.aicommerce.storefront.returns;

import java.math.BigDecimal;

/**
 * Structured result returned to the model by initiateReturn / issueExchangeVoucher.
 * The model explains it; it cannot change it.
 *
 * @param alreadyProcessed true when an earlier identical request created the RMA/voucher (idempotent replay)
 * @param nextStep         guidance for the model on what to do with this result
 */
public record ReturnDecision(
        DecisionStatus status,
        String orderId,
        String sku,
        String productName,
        String category,
        Long daysSinceDelivery,
        String rmaId,
        String voucherCode,
        BigDecimal voucherValue,
        Integer creditPercent,
        String policyRef,
        String message,
        String nextStep,
        boolean alreadyProcessed) {

    static ReturnDecision notFound(String orderId, String sku) {
        return new ReturnDecision(DecisionStatus.NOT_FOUND, orderId, sku, null, null, null, null, null, null, null,
                null, "No order " + orderId + (sku == null ? "" : " with item " + sku)
                        + " was found in this customer's account.",
                "Tell the customer the order was not found in their account. Do not speculate about other accounts.",
                false);
    }
}
