package io.github.asmitmans.iotbackend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final String username;
    private final String password;
    private final boolean enabled;
    private final UUID accountId;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(String username, String password, boolean enabled, UUID accountId, Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.accountId = accountId;
        this.authorities = authorities;
    }

    public UUID getAccountId() {
        return accountId;
    }

    @Override public String getUsername() {
        return username;
    }

    @Override public String getPassword() {
        return password;
    }

    @Override public boolean isEnabled() {
        return enabled;
    }

    @Override public boolean isAccountNonExpired() {
        return true;
    }

    @Override public boolean isAccountNonLocked() {
        return true;
    }

    @Override public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

}