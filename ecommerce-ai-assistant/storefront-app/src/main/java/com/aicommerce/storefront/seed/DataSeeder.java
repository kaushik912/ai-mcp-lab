package com.aicommerce.storefront.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final DemoDataService demoData;

    public DataSeeder(DemoDataService demoData) {
        this.demoData = demoData;
    }

    @Override
    public void run(String... args) {
        demoData.reset();
        log.info("Demo data seeded: 2 users, 8 products, 6 orders");
    }
}
