package com.roadmap.securevault.tenant.entity;

import com.roadmap.securevault.common.entity.BaseRefreshToken;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class TenantRefreshToken extends BaseRefreshToken<TenantUser> {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private TenantUser user;
}