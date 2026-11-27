package com.aicommerce.storefront.tools;

import com.aicommerce.storefront.domain.Category;
import com.aicommerce.storefront.repo.ProductRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Deterministic catalog lookups: live price and stock straight from the database. */
@Component
public class ProductCatalogTools {

    public record ProductView(String sku, String name, String category, BigDecimal price, int stockCount,
                              boolean inStock, String specs) {}

    public record SearchResult(List<ProductView> products, String note) {}

    private final ProductRepository products;

    public ProductCatalogTools(ProductRepository products) {
        this.products = products;
    }

    @Tool(description = """
            Search the live product catalog with exact filters. Returns current price (₹) and stock count.
            Use for any question about price, availability, or browsing a category. All filters are optional.""")
    public SearchResult searchProducts(
            @ToolParam(required = false, description = "One of ELECTRONICS, APPAREL, FOOTWEAR, HYGIENE") String category,
            @ToolParam(required = false, description = "Maximum price in rupees, e.g. 8000") Double maxPrice,
            @ToolParam(required = false, description = "true to only return products with stock > 0") Boolean inStockOnly) {
        Category cat = null;
        if (category != null && !category.isBlank()) {
            try {
                cat = Category.valueOf(category.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return new SearchResult(List.of(), "Unknown category '" + category + "'. Valid: "
                        + Arrays.toString(Category.values()));
            }
        }
        List<ProductView> hits = products.search(cat, maxPrice == null ? null : BigDecimal.valueOf(maxPrice),
                        Boolean.TRUE.equals(inStockOnly)).stream()
                .map(p -> new ProductView(p.getSku(), p.getName(), p.getCategory().name(), p.getPrice(),
                        p.getStockCount(), p.getStockCount() > 0, p.getSpecsDescription()))
                .toList();
        return new SearchResult(hits, hits.isEmpty() ? "No products match these filters." : null);
    }
}
