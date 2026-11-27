package com.aicommerce.storefront.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Two ways in, one identity source:
 *  - Browser: session login via the demo user switcher (DemoAuthController), CSRF-protected.
 *  - curl/tests: HTTP Basic (alex/demo, priya/demo). CSRF is skipped ONLY for requests carrying
 *    Basic credentials: browsers never attach those cross-site on their own, so they can't be forged.
 * The authenticated principal's username is the ONLY source of user identity in this app.
 */
@Configuration
public class SecurityConfig {

    static final RequestMatcher BASIC_AUTH_HEADER = req -> {
        String h = req.getHeader("Authorization");
        return h != null && h.regionMatches(true, 0, "Basic ", 0, 6);
    };

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SecurityContextRepository contextRepository) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                    .requestMatchers("/login", "/demo/login/**", "/js/**", "/css/**", "/favicon.ico", "/error").permitAll()
                    .anyRequest().authenticated())
            .securityContext(sc -> sc.securityContextRepository(contextRepository))
            .httpBasic(Customizer.withDefaults())
            .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint()))
            .csrf(csrf -> csrf.ignoringRequestMatchers(BASIC_AUTH_HEADER))
            .logout(l -> l.logoutSuccessUrl("/login"));
        return http.build();
    }

    /** API calls get a 401 (so curl sees it); page requests are redirected to the login page. */
    private static AuthenticationEntryPoint entryPoint() {
        BasicAuthenticationEntryPoint basic = new BasicAuthenticationEntryPoint();
        basic.setRealmName("storefront");
        LoginUrlAuthenticationEntryPoint login = new LoginUrlAuthenticationEntryPoint("/login");
        return (req, res, ex) -> {
            if (isApi(req) && req.getHeader("X-Requested-With") != null) {
                // UI fetch: bare 401 (no WWW-Authenticate), so the browser doesn't pop a Basic-auth dialog.
                res.sendError(401);
            } else if (isApi(req)) {
                basic.commence(req, res, ex);
            } else {
                login.commence(req, res, ex);
            }
        };
    }

    private static boolean isApi(HttpServletRequest req) {
        return req.getRequestURI().startsWith("/api/");
    }

    @Bean
    UserDetailsService demoUsers() {
        return new InMemoryUserDetailsManager(
                User.withUsername("alex").password("{noop}demo").roles("USER", "VIP").build(),
                User.withUsername("priya").password("{noop}demo").roles("USER").build());
    }
}
