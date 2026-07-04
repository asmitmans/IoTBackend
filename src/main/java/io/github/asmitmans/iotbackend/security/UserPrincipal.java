package io.github.asmitmans.iotbackend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class UserPrincipal implements UserDetails {

    private final String username;
    private final String password;
    private final boolean enabled;
    private final Long companyId;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(String username, String password, boolean enabled, Long companyId, Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.companyId = companyId;
        this.authorities = authorities;
    }

    public Long getCompanyId() {
        return companyId;
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
