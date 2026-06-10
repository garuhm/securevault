package com.roadmap.securevault.platform.entity;

import com.roadmap.securevault.common.entity.BaseRefreshToken;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "platform_refresh_tokens", schema = "public")
@Getter @Setter @NoArgsConstructor @SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class PlatformRefreshToken extends BaseRefreshToken<PlatformUser> {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private PlatformUser user;
}
