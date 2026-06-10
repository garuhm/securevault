package com.roadmap.securevault.tenant.entity;

import com.roadmap.securevault.common.entity.BaseEntity;
import com.roadmap.securevault.tenant.entity.enums.TenantStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "tenants", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class Tenant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false, unique = true, length = 32)
    private String companyCode;

    @Column(nullable = false, unique = true)
    private String schemaName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status;

    private String ownerEmail;

    @PrePersist
    protected void prePersist() {
        if (status == null) status = TenantStatus.PENDING;
    }
}