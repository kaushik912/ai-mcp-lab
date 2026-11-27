package com.aicommerce.storefront.web;

import com.aicommerce.storefront.domain.Category;
import com.aicommerce.storefront.repo.ProductRepository;
import com.aicommerce.storefront.returns.OrderViews.OrderView;
import com.aicommerce.storefront.returns.ReturnService;
import com.aicommerce.storefront.security.CurrentUserService;
import com.aicommerce.storefront.seed.DemoDataService;
import com.aicommerce.storefront.tools.ProductCatalogTools.ProductView;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * JSON for the single-page UI. Same rule as the tools: "my" data comes from the logged-in user,
 * never from a request parameter.
 */
@RestController
@RequestMapping("/api")
public class StorefrontApiController {

    private static final Logger log = LoggerFactory.getLogger(StorefrontApiController.class);

    private final CurrentUserService currentUser;
    private final ReturnService returns;
    private final ProductRepository products;
    private final DemoDataService demoData;
    private final ChatMemoryRepository chatMemory;

    public StorefrontApiController(CurrentUserService currentUser, ReturnService returns, ProductRepository products,
                                   DemoDataService demoData, ChatMemoryRepository chatMemory) {
        this.currentUser = currentUser;
        this.returns = returns;
        this.products = products;
        this.demoData = demoData;
        this.chatMemory = chatMemory;
    }

    @GetMapping("/me")
    public CurrentUserService.AuthenticatedUser me() {
        return currentUser.require();
    }

    @GetMapping("/products")
    public List<ProductView> products(@RequestParam(required = false) Category category) {
        return products.search(category, null, false).stream()
                .map(p -> new ProductView(p.getSku(), p.getName(), p.getCategory().name(), p.getPrice(),
                        p.getStockCount(), p.getStockCount() > 0, p.getSpecsDescription()))
                .toList();
    }

    @GetMapping("/orders")
    public List<OrderView> myOrders() {
        return returns.myOrders(currentUser.require().id());
    }

    /** Re-seeds all demo data and forgets every chat conversation. */
    @PostMapping("/demo/reset")
    public Map<String, Object> reset() {
        demoData.reset();
        List<String> ids = chatMemory.findConversationIds();
        ids.forEach(chatMemory::deleteByConversationId);
        log.info("Demo reset by {}: data re-seeded, {} conversations cleared", currentUser.require().username(), ids.size());
        return Map.of("reset", true, "conversationsCleared", ids.size());
    }
}
