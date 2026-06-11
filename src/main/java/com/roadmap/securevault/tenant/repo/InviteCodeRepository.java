package com.roadmap.securevault.tenant.repo;

import com.roadmap.securevault.tenant.entity.InviteCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface InviteCodeRepository extends
        JpaRepository<InviteCode, UUID>,
        JpaSpecificationExecutor<InviteCode>
{}
