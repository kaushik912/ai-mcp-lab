package com.aicommerce.storefront.security;

import com.aicommerce.storefront.domain.AppUser;
import com.aicommerce.storefront.repo.AppUserRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** The only place user identity is derived: the authenticated Spring Security principal -> AppUser row. */
@Service
public class CurrentUserService {

    public record AuthenticatedUser(Long id, String username, String displayName, String tier) {}

    private final AppUserRepository users;

    public CurrentUserService(AppUserRepository users) {
        this.users = users;
    }

    public AuthenticatedUser require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Not authenticated");
        }
        AppUser u = users.findByUsername(auth.getName())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Unknown user " + auth.getName()));
        return new AuthenticatedUser(u.getId(), u.getUsername(), u.getDisplayName(), u.getLoyaltyTier().name());
    }
}
