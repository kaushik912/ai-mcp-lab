package com.aicommerce.storefront.returns;

/** Machine-readable outcome of a return or voucher request. Computed in Java, never by the model. */
public enum DecisionStatus {
    /** Return accepted; an RMA was created (or the existing one returned). */
    APPROVED,
    /** Past every window for this category. */
    REJECTED_OUTSIDE_WINDOW,
    /** Inside the window but excluded by a rule (opened in-ear audio not defective, opened hygiene). */
    REJECTED_NON_RETURNABLE,
    /** Day 31-45 apparel/footwear: no refund, but an exchange/store-credit voucher can be issued. */
    ELIGIBLE_FOR_EXCHANGE_CREDIT,
    /** Voucher created (or the existing one returned). */
    VOUCHER_ISSUED,
    /** Voucher asked for an item that doesn't qualify (wrong category, still in refund window, already refunded). */
    NOT_ELIGIBLE_FOR_VOUCHER,
    /** Order not delivered yet; nothing to return. */
    NOT_DELIVERED,
    /** No such order/item in the authenticated customer's account (also used for other customers' orders). */
    NOT_FOUND
}
