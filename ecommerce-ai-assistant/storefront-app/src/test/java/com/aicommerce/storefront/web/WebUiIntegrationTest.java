package com.aicommerce.storefront.web;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aicommerce.storefront.repo.AppUserRepository;
import com.aicommerce.storefront.seed.DemoDataService;
import com.aicommerce.storefront.tools.OrderAndReturnTools;
import com.aicommerce.storefront.tools.ToolIdentity;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/** Phase 5 web layer: login/switcher, CSRF, owner-scoped JSON APIs, reset, page rendering. No LLM. */
@SpringBootTest(properties = "app.rag.ingest-on-startup=false")
@AutoConfigureMockMvc
class WebUiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired DemoDataService demoData;
    @Autowired OrderAndReturnTools tools;
    @Autowired AppUserRepository users;

    @BeforeEach
    void reset() {
        demoData.reset();
    }

    private MockHttpSession loginAs(String user) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/demo/login/" + user).session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        return session;
    }

    @Test
    void pageRequiresLoginAndLoginPageOffersBothUsers() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Alex"), containsString("Priya"))));
    }

    @Test
    void oneClickLoginRendersUserBadgeAndUi() throws Exception {
        MockHttpSession s = loginAs("alex");
        mvc.perform(get("/").session(s)).andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Alex - VIP"), containsString("Reset Demo Data"),
                        containsString("Agent X-Ray"), containsString("name=\"_csrf\""))));
    }

    @Test
    void switchUserRotatesSessionAndChangesWhoseOrdersAreShown() throws Exception {
        MockHttpSession s = loginAs("alex");
        mvc.perform(get("/api/orders").session(s))
                .andExpect(jsonPath("$[*].orderId", containsInAnyOrder("ORD-1001", "ORD-1002", "ORD-1003", "ORD-1004")));

        String before = s.getId();
        mvc.perform(post("/demo/login/priya").session(s).with(csrf())).andExpect(redirectedUrl("/"));
        org.assertj.core.api.Assertions.assertThat(s.getId()).isNotEqualTo(before); // session fixation defence

        mvc.perform(get("/api/orders").session(s))
                .andExpect(jsonPath("$[*].orderId", containsInAnyOrder("ORD-2001", "ORD-2002")));
        mvc.perform(get("/").session(s)).andExpect(content().string(containsString("Priya - Standard")));
    }

    @Test
    void loginAndSwitchNeedCsrfToken() throws Exception {
        mvc.perform(post("/demo/login/alex")).andExpect(status().isForbidden());
    }

    @Test
    void unknownDemoUserIsRejected() throws Exception {
        mvc.perform(post("/demo/login/mallory").with(csrf())).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void ordersApiIgnoresAnyUserParameter() throws Exception {
        mvc.perform(get("/api/orders").param("userId", "2").param("username", "priya").with(httpBasic("alex", "demo")))
                .andExpect(jsonPath("$[*].orderId", not(hasItem("ORD-2001"))))
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void apiUnauthenticatedIs401_andUiFetchGetsNoBasicChallenge() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
        mvc.perform(get("/api/orders").header("X-Requested-With", "fetch")).andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void sessionWritesNeedCsrf_basicAuthWritesDont() throws Exception {
        MockHttpSession s = loginAs("alex");
        mvc.perform(post("/api/demo/reset").session(s)).andExpect(status().isForbidden());
        mvc.perform(post("/api/demo/reset").session(s).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/demo/reset").with(httpBasic("alex", "demo"))).andExpect(status().isOk());
    }

    @Test
    void resetRestoresSeedData() throws Exception {
        ToolContext alex = new ToolContext(Map.of(ToolIdentity.USER_ID, users.findByUsername("alex").orElseThrow().getId()));
        tools.initiateReturn("ORD-1001", null, "x", false, alex);
        mvc.perform(get("/api/orders").with(httpBasic("alex", "demo")))
                .andExpect(jsonPath("$[?(@.orderId=='ORD-1001')].items[0].returnStatus").value(hasItem("RETURN_APPROVED")));
        mvc.perform(get("/api/products").param("category", "FOOTWEAR").with(httpBasic("alex", "demo")))
                .andExpect(jsonPath("$[?(@.sku=='SUMMIT-SHOE-01')].stockCount").value(hasItem(15)));

        mvc.perform(post("/api/demo/reset").with(httpBasic("alex", "demo")))
                .andExpect(jsonPath("$.reset").value(true));

        mvc.perform(get("/api/orders").with(httpBasic("alex", "demo")))
                .andExpect(jsonPath("$[?(@.orderId=='ORD-1001')].items[0].returnStatus").value(hasItem("NONE")));
        mvc.perform(get("/api/products").param("category", "FOOTWEAR").with(httpBasic("alex", "demo")))
                .andExpect(jsonPath("$[?(@.sku=='SUMMIT-SHOE-01')].stockCount").value(hasItem(14)));
    }

    @Test
    void productsApiFiltersByCategory() throws Exception {
        mvc.perform(get("/api/products").param("category", "FOOTWEAR").with(httpBasic("alex", "demo")))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/products").with(httpBasic("alex", "demo")))
                .andExpect(jsonPath("$.length()").value(8));
    }

    @Test
    void staticAssetsServed() throws Exception {
        mvc.perform(get("/js/app.js")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Agent X-Ray")));
    }
}
