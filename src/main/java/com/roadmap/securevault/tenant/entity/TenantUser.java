package com.roadmap.securevault.tenant.entity;

import com.roadmap.securevault.common.entity.BaseUser;
import com.roadmap.securevault.tenant.entity.enums.TenantRole;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")

@Getter @Setter @NoArgsConstructor @SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class TenantUser extends BaseUser implements UserDetails {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantRole role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TenantRefreshToken> refreshTokens;

    @PrePersist
    protected void prePersist() {
        if (refreshTokens == null) refreshTokens = new HashSet<>();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(role);
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
