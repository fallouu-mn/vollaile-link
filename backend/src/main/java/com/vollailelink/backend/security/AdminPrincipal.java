package com.vollailelink.backend.security;

import com.vollailelink.backend.model.Administrator;
import com.vollailelink.backend.model.enums.AdminStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AdminPrincipal implements UserDetails {

    private final Long id;
    private final String phone;
    private final String passwordHash;
    private final boolean active;
    private final boolean mustChangePassword;
    private final boolean accountNonLocked;

    public AdminPrincipal(Administrator administrator) {
        this.id = administrator.getId();
        this.phone = administrator.getPhone();
        this.passwordHash = administrator.getPasswordHash();
        this.active = administrator.getStatus() == AdminStatus.ACTIVE;
        this.mustChangePassword = Boolean.TRUE.equals(administrator.getMustChangePassword());
        this.accountNonLocked = administrator.getLockedUntil() == null
                || !administrator.getLockedUntil().isAfter(java.time.OffsetDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
