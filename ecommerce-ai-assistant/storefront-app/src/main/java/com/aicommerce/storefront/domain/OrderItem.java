package com.aicommerce.storefront.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_item", uniqueConstraints = @UniqueConstraint(columnNames = {"orderId", "sku"}))
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    private int quantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal pricePaid;

    /** Seal broken / product used. Drives the hygiene & opened-electronics rules. */
    private boolean boxOpened;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnStatus returnStatus = ReturnStatus.NONE;

    /** RMA id once a return is approved. One per item: the idempotency anchor for initiateReturn. */
    private String rmaId;

    /** Exchange / store-credit voucher (31-45 day grace policy). One per item: idempotency anchor for issueExchangeVoucher. */
    private String voucherCode;

    @Column(precision = 10, scale = 2)
    private BigDecimal voucherValue;

    protected OrderItem() {}

    public OrderItem(String orderId, String sku, String productName, Category category, int quantity,
                     BigDecimal pricePaid, boolean boxOpened) {
        this.orderId = orderId;
        this.sku = sku;
        this.productName = productName;
        this.category = category;
        this.quantity = quantity;
        this.pricePaid = pricePaid;
        this.boxOpened = boxOpened;
    }

    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public Category getCategory() { return category; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPricePaid() { return pricePaid; }
    public boolean isBoxOpened() { return boxOpened; }
    public ReturnStatus getReturnStatus() { return returnStatus; }
    public String getRmaId() { return rmaId; }
    public void setReturnStatus(ReturnStatus returnStatus) { this.returnStatus = returnStatus; }
    public void setRmaId(String rmaId) { this.rmaId = rmaId; }
    public String getVoucherCode() { return voucherCode; }
    public BigDecimal getVoucherValue() { return voucherValue; }

    public void issueVoucher(String code, BigDecimal value) {
        this.voucherCode = code;
        this.voucherValue = value;
        this.returnStatus = ReturnStatus.EXCHANGE_ISSUED;
    }
}
