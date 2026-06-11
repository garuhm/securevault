package com.roadmap.securevault.tenant.entity;

import com.roadmap.securevault.common.entity.BaseEntity;
import com.roadmap.securevault.common.entity.UserOwnable;
import com.roadmap.securevault.tenant.entity.enums.TenantRole;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invite_codes")

@Getter @Setter @NoArgsConstructor @SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class InviteCode extends BaseEntity implements UserOwnable<TenantUser> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private TenantUser user;

    @Column(nullable = false)
    private String inviteeEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantRole role;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private TenantUser createdBy;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime usedAt;

    @PrePersist
    public void prePersist() { if (role == null) role = TenantRole.TENANT_MEMBER; }

    @Override
    public TenantUser getUser() {
        return createdBy;
    }
}
