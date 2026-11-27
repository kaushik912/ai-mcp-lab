package com.aicommerce.storefront.tools;

import com.aicommerce.storefront.returns.OrderViews.OrderView;
import com.aicommerce.storefront.returns.OrderViews.ProfileView;
import com.aicommerce.storefront.returns.ReturnDecision;
import com.aicommerce.storefront.returns.ReturnService;
import java.util.List;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Order & return tools. Note what is NOT here: no userId / username parameter anywhere.
 * The authenticated user arrives via ToolContext (see ToolIdentity) and every query is owner-scoped.
 */
@Component
public class OrderAndReturnTools {

    private final ReturnService returns;

    public OrderAndReturnTools(ReturnService returns) {
        this.returns = returns;
    }

    @Tool(description = """
            List the signed-in customer's own orders with items, delivery dates, days since delivery, return/RMA status,
            shipment tracking id, and whether the order qualifies for the late-delivery coupon.
            Only ever returns the signed-in customer's orders.""")
    public List<OrderView> getMyOrders(ToolContext ctx) {
        return returns.myOrders(ToolIdentity.userId(ctx));
    }

    @Tool(description = "Get the signed-in customer's display name, loyalty tier, city and pincode (needed for return labels and drop-off points).")
    public ProfileView getMyProfile(ToolContext ctx) {
        return returns.profile(ToolIdentity.userId(ctx));
    }

    @Tool(description = """
            Request a return/refund for one item of one of the customer's orders. Eligibility (return window, hygiene
            and opened-box rules) is decided by the system from the real delivery date; the returned status is final.
            Calling it again for the same item returns the same RMA (no duplicates).""")
    public ReturnDecision initiateReturn(
            @ToolParam(description = "Order id, e.g. ORD-1001") String orderId,
            @ToolParam(required = false, description = "Item SKU or product name; may be omitted if the order has a single item") String sku,
            @ToolParam(required = false, description = "Customer's reason for returning") String reason,
            @ToolParam(description = "true only if the customer said the product is faulty/defective") boolean isDefective,
            ToolContext ctx) {
        return returns.initiateReturn(ToolIdentity.userId(ctx), orderId, sku, reason, isDefective);
    }

    @Tool(description = """
            Issue an exchange / store-credit voucher for an apparel or footwear item delivered 31-45 days ago
            (80% of price paid, 90% for VIP). Only call after the customer agrees to a voucher.
            Calling it again for the same item returns the same voucher code (no duplicates).""")
    public ReturnDecision issueExchangeVoucher(
            @ToolParam(description = "Order id, e.g. ORD-1003") String orderId,
            @ToolParam(required = false, description = "Item SKU or product name; may be omitted if the order has a single item") String sku,
            ToolContext ctx) {
        return returns.issueExchangeVoucher(ToolIdentity.userId(ctx), orderId, sku);
    }
}
