package com.aicommerce.storefront.web;

import com.aicommerce.storefront.security.CurrentUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** The single-page storefront. The user badge is rendered server-side from the authenticated principal. */
@Controller
public class PageController {

    private final CurrentUserService currentUser;

    public PageController(CurrentUserService currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("user", currentUser.require());
        return "index";
    }
}
