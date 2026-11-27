package com.aicommerce.storefront.seed;

import com.aicommerce.storefront.domain.*;
import com.aicommerce.storefront.repo.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Seeds (and re-seeds) the demo dataset. Dates are relative to "now" so day-ages stay exact. */
@Service
public class DemoDataService {

    private final AppUserRepository users;
    private final ProductRepository products;
    private final CustomerOrderRepository orders;
    private final OrderItemRepository items;

    public DemoDataService(AppUserRepository users, ProductRepository products,
                           CustomerOrderRepository orders, OrderItemRepository items) {
        this.users = users;
        this.products = products;
        this.orders = orders;
        this.items = items;
    }

    @Transactional
    public void reset() {
        items.deleteAllInBatch();
        orders.deleteAllInBatch();
        products.deleteAllInBatch();
        users.deleteAllInBatch();
        seed();
    }

    private void seed() {
        AppUser alex = users.save(new AppUser("alex", "Alex", LoyaltyTier.VIP, "Bangalore", "560100"));
        AppUser priya = users.save(new AppUser("priya", "Priya", LoyaltyTier.STANDARD, "Mumbai", "400001"));

        Product buds = product("AERO-BUDS-01", "AeroBuds Pro Wireless Earbuds", Category.ELECTRONICS, "5999", 25,
                "ANC wireless earbuds, 32h battery, IPX4, Bluetooth 5.3, in-ear hygiene seal.", "aerobuds");
        Product shoes = product("SUMMIT-SHOE-01", "SummitGrip Waterproof Trail Shoes", Category.FOOTWEAR, "7499", 14,
                "Gore-Tex style waterproof membrane, Vibram-type lugged outsole for mud and wet rock.", "summitgrip");
        Product jacket = product("NIMBUS-JKT-01", "Nimbus Down Jacket", Category.APPAREL, "12999", 6,
                "800-fill water-resistant down jacket, packable, rated to -10C.", "nimbus");
        Product brush = product("PURE-BRUSH-01", "PureSonic Electric Toothbrush", Category.HYGIENE, "2499", 40,
                "Sonic toothbrush, 5 modes, 30-day battery, 2 brush heads included.", "puresonic");
        Product pack = product("TRAIL-HYDRO-01", "TrailBlazer Hydration Pack", Category.APPAREL, "3499", 18,
                "12L trail running vest with 2L bladder, reflective trim, chest-strap whistle.", "trailblazer");
        Product bank = product("VOLT-BANK-01", "VoltEdge 20000mAh Power Bank", Category.ELECTRONICS, "2999", 32,
                "20000mAh, 65W USB-C PD, dual output, airline-safe.", "voltedge");
        Product runners = product("URBAN-RUN-01", "UrbanStride Velocity Running Shoes", Category.FOOTWEAR, "6499", 12,
                "Breathable mesh road-running shoe, carbon-infused midsole, 240g.", "urbanstride");
        Product baselayer = product("THERMO-BASE-01", "ThermoMerino Base Layer Set", Category.APPAREL, "3999", 0,
                "Merino wool thermal top and leggings. Currently out of stock.", "thermomerino");

        Instant now = Instant.now();

        // --- alex: one order per demo scenario ---
        // ORD-1001: delivered 4 days ago, trail shoes -> fully returnable
        order("ORD-1001", alex, now.minus(Duration.ofDays(7)), now.minus(Duration.ofDays(4)),
                now.minus(Duration.ofDays(4)), OrderStatus.DELIVERED, "TRK-BLR-10010", shoes, false);
        // ORD-1002: delivered 6 days ago, earbuds, box already opened -> only returnable if defective
        order("ORD-1002", alex, now.minus(Duration.ofDays(9)), now.minus(Duration.ofDays(6)),
                now.minus(Duration.ofDays(6)), OrderStatus.DELIVERED, "TRK-BLR-10020", buds, true);
        // ORD-1003: delivered 36 days ago -> outside 30d refund, inside 31-45d exchange-credit window
        order("ORD-1003", alex, now.minus(Duration.ofDays(40)), now.minus(Duration.ofDays(36)),
                now.minus(Duration.ofDays(36)), OrderStatus.DELIVERED, "TRK-BLR-10030", jacket, false);
        // ORD-1004: delayed at carrier hub; promised 3 days ago (>24h late -> Rs 500 coupon rule)
        order("ORD-1004", alex, now.minus(Duration.ofDays(8)), now.minus(Duration.ofDays(3)),
                null, OrderStatus.DELAYED, "TRK-BLR-77412", pack, false);

        // --- priya: for cross-user isolation / prompt-injection demo ---
        order("ORD-2001", priya, now.minus(Duration.ofDays(13)), now.minus(Duration.ofDays(10)),
                now.minus(Duration.ofDays(10)), OrderStatus.DELIVERED, "TRK-MUM-20010", jacket, false);
        order("ORD-2002", priya, now.minus(Duration.ofDays(2)), now.plus(Duration.ofDays(2)),
                null, OrderStatus.IN_TRANSIT, "TRK-MUM-20020", bank, false);
    }

    private Product product(String sku, String name, Category cat, String price, int stock, String specs, String img) {
        return products.save(new Product(sku, name, cat, new BigDecimal(price), stock, specs,
                "https://placehold.co/400x300?text=" + img));
    }

    private void order(String id, AppUser user, Instant ordered, Instant promised, Instant delivered,
                       OrderStatus status, String tracking, Product p, boolean boxOpened) {
        orders.save(new CustomerOrder(id, user.getId(), ordered, promised, delivered, status, tracking));
        items.save(new OrderItem(id, p.getSku(), p.getName(), p.getCategory(), 1, p.getPrice(), boxOpened));
    }
}
