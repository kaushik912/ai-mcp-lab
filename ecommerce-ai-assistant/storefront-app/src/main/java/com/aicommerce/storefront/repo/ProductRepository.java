package com.aicommerce.storefront.repo;

import com.aicommerce.storefront.domain.Category;
import com.aicommerce.storefront.domain.Product;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, String> {

    /** Deterministic catalog filter; any null argument means "don't filter on it". */
    @Query("""
            select p from Product p
            where (:category is null or p.category = :category)
              and (:maxPrice is null or p.price <= :maxPrice)
              and (:inStockOnly = false or p.stockCount > 0)
            order by p.price asc
            """)
    List<Product> search(@Param("category") Category category,
                         @Param("maxPrice") BigDecimal maxPrice,
                         @Param("inStockOnly") boolean inStockOnly);
}
