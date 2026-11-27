package com.aicommerce.storefront;

import static org.assertj.core.api.Assertions.assertThat;

import com.aicommerce.storefront.domain.*;
import com.aicommerce.storefront.repo.*;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.rag.ingest-on-startup=false") // no paid embedding calls for DB tests
class DemoDataSeedTest {

    @Autowired AppUserRepository users;
    @Autowired ProductRepository products;
    @Autowired CustomerOrderRepository orders;
    @Autowired OrderItemRepository items;

    @Test
    void seedsUsersProductsAndOrders() {
        assertThat(users.count()).isEqualTo(2);
        assertThat(products.count()).isEqualTo(8);
        assertThat(orders.count()).isEqualTo(6);
        assertThat(users.findByUsername("alex").orElseThrow().getLoyaltyTier()).isEqualTo(LoyaltyTier.VIP);
        assertThat(users.findByUsername("priya").orElseThrow().getPincode()).isEqualTo("400001");
    }

    @Test
    void alexOrdersHaveExpectedAges() {
        Long alex = users.findByUsername("alex").orElseThrow().getId();
        assertThat(orders.findAllForUser(alex)).hasSize(4);
        assertThat(daysSinceDelivery("ORD-1001", alex)).isEqualTo(4);
        assertThat(daysSinceDelivery("ORD-1002", alex)).isEqualTo(6);
        assertThat(daysSinceDelivery("ORD-1003", alex)).isEqualTo(36);
        assertThat(orders.findByIdForUser("ORD-1004", alex).orElseThrow().getStatus()).isEqualTo(OrderStatus.DELAYED);
    }

    @Test
    void orderLookupIsScopedToOwner() {
        Long alex = users.findByUsername("alex").orElseThrow().getId();
        Long priya = users.findByUsername("priya").orElseThrow().getId();
        assertThat(orders.findByIdForUser("ORD-2001", alex)).isEmpty();
        assertThat(orders.findByIdForUser("ORD-2001", priya)).isPresent();
        assertThat(items.findByOrderForUser("ORD-2001", alex)).isEmpty();
    }

    @Test
    void catalogSearchFiltersDeterministically() {
        var hits = products.search(Category.FOOTWEAR, new java.math.BigDecimal("8000"), true);
        assertThat(hits).extracting(Product::getName).contains("SummitGrip Waterproof Trail Shoes");
        assertThat(products.search(Category.APPAREL, null, true))
                .extracting(Product::getName).doesNotContain("ThermoMerino Base Layer Set");
    }

    private long daysSinceDelivery(String orderId, Long userId) {
        Instant d = orders.findByIdForUser(orderId, userId).orElseThrow().getDeliveredAt();
        return Duration.between(d, Instant.now()).toDays();
    }
}
