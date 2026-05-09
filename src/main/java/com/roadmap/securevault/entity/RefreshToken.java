package com.roadmap.securevault.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private Date expiryDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @ManyToOne()
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
