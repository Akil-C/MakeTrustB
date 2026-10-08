package com.markettrust.security;

import com.markettrust.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Spring Security {@link UserDetails} wrapper around the MarketTrust {@link User} entity.
 * Exposes the numeric user ID so that {@link SecurityUtils#getCurrentUserId()} can extract it.
 */
@Getter
@Builder
@AllArgsConstructor
public class MarketTrustUserDetails implements UserDetails {

    private final Long   id;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public MarketTrustUserDetails(User user) {
        this.id       = user.getId();
        this.email    = user.getEmail();
        this.password = user.getPassword();
        this.enabled  = user.getStatus() != null && user.getStatus().name().equals("ACTIVE");
        this.authorities = user.getRoles() == null ? java.util.Collections.emptyList() :
                user.getRoles().stream()
                        .flatMap(r -> {
                            String raw = r.getName().name();
                            String roleName = raw.startsWith("ROLE_") ? raw.substring(5) : raw;
                            return java.util.stream.Stream.of(
                                    new SimpleGrantedAuthority("ROLE_" + roleName),
                                    new SimpleGrantedAuthority(roleName)
                            );
                        })
                        .collect(Collectors.toList());
    }

    public Long getId() { return id; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
