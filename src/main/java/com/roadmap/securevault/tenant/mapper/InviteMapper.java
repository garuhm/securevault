package com.roadmap.securevault.tenant.mapper;

import com.roadmap.securevault.tenant.dto.invite.InviteResponse;
import com.roadmap.securevault.tenant.entity.InviteCode;

public class InviteMapper {
    public static InviteResponse toResponse(InviteCode inviteCode, String includedInviteCodeString) {
        // invite response is a record, use new
        return new InviteResponse(
                inviteCode.getId(),
                includedInviteCodeString,
                inviteCode.getRole(),
                inviteCode.getInviteeEmail(),
                inviteCode.getExpiresAt(),
                inviteCode.getUsedAt(),
                inviteCode.getCreatedAt()
        );
    }
}
