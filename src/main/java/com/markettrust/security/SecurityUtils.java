package com.markettrust.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Static utility class for accessing the current authenticated principal
 * from the Spring Security context.
 *
 * <p>All methods throw {@link AccessDeniedException} when called outside of an
 * authenticated request, so they should only be used in secured endpoints.</p>
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Prevent instantiation
    }

    // ---------------------------------------------------------------------------
    // Principal accessors
    // ---------------------------------------------------------------------------

    /**
     * Returns the numeric ID of the currently authenticated user.
     *
     * @return user ID
     * @throws AccessDeniedException if no authentication is present
     */
    public static Long getCurrentUserId() {
        Authentication auth = requireAuthentication();
        Object principal = auth.getPrincipal();

        if (principal instanceof MarketTrustUserDetails userDetails) {
            return userDetails.getId();
        }
        throw new AccessDeniedException("Cannot resolve user ID from principal: " + principal.getClass());
    }

    public static Long getCurrentUserIdSafely() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof MarketTrustUserDetails userDetails) {
                return userDetails.getId();
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Returns the email (username) of the currently authenticated user.
     *
     * @return email address
     * @throws AccessDeniedException if no authentication is present
     */
    public static String getCurrentUserEmail() {
        Authentication auth = requireAuthentication();
        Object principal = auth.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String email) {
            return email;
        }
        throw new AccessDeniedException("Cannot resolve email from principal: " + principal.getClass());
    }

    // ---------------------------------------------------------------------------
    // Role / authority checks
    // ---------------------------------------------------------------------------

    /**
     * Checks whether the current user has the given role (prefix-agnostic).
     * Both {@code "ADMIN"} and {@code "ROLE_ADMIN"} are matched.
     *
     * @param role the role name to check (e.g. {@code "ADMIN"} or {@code "ROLE_ADMIN"})
     * @return {@code true} if the user holds the role
     */
    public static boolean hasRole(String role) {
        Authentication auth = getAuthentication();
        if (auth == null) return false;

        String normalised = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(role) || a.equals(normalised));
    }

    /**
     * Convenience check — returns {@code true} if the current user is an ADMIN.
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Convenience check — returns {@code true} if the current user is a SELLER.
     */
    public static boolean isSeller() {
        return hasRole("SELLER");
    }

    /**
     * Convenience check — returns {@code true} if the current user is a BUYER.
     */
    public static boolean isBuyer() {
        return hasRole("BUYER");
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    /**
     * Returns the current {@link Authentication} or {@code null} if unauthenticated.
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static Authentication requireAuthentication() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("No authenticated principal in SecurityContext");
        }
        return auth;
    }
}
