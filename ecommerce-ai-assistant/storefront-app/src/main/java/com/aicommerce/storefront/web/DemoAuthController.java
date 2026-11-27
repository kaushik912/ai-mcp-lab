package com.aicommerce.storefront.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Demo-only one-click login / user switcher (alex <-> priya). It skips the password on purpose so the
 * presenter can switch live; remove it in a real deployment. It is still CSRF-protected (POST + token),
 * rotates the session id on every switch (session-fixation defence, and a fresh chat memory key),
 * and only accepts the two seeded demo users.
 */
@Controller
public class DemoAuthController {

    private static final Logger log = LoggerFactory.getLogger(DemoAuthController.class);
    private static final Set<String> DEMO_USERS = Set.of("alex", "priya");

    private final UserDetailsService users;
    private final SecurityContextRepository contextRepository;

    public DemoAuthController(UserDetailsService users, SecurityContextRepository contextRepository) {
        this.users = users;
        this.contextRepository = contextRepository;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/demo/login/{username}")
    public String loginAs(@PathVariable String username, HttpServletRequest request, HttpServletResponse response) {
        if (!DEMO_USERS.contains(username)) {
            return "redirect:/login?error";
        }
        UserDetails user = users.loadUserByUsername(username);
        request.getSession(true);
        request.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        log.info("Demo login: session now authenticated as {}", username);
        return "redirect:/";
    }
}
