package com.aicommerce.storefront.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class Product {

    @Id
    private String sku;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stockCount;

    @Column(length = 2000)
    private String specsDescription;

    private String imageUrl;

    protected Product() {}

    public Product(String sku, String name, Category category, BigDecimal price, int stockCount,
                   String specsDescription, String imageUrl) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stockCount = stockCount;
        this.specsDescription = specsDescription;
        this.imageUrl = imageUrl;
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public int getStockCount() { return stockCount; }
    public String getSpecsDescription() { return specsDescription; }
    public String getImageUrl() { return imageUrl; }
    public void setStockCount(int stockCount) { this.stockCount = stockCount; }
}
