package com.yolt.clients.clientgroup.admins.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
public class ClientGroupAdminDTO {
    UUID portalUserId;
    String portalUserName;
    String portalUserOrganisation;
    String portalUserEmail;
    UUID inviteId;
    String inviteName;
    String inviteEmail;
    LocalDateTime inviteCreatedAt;
    LocalDateTime inviteUsedAt;
    LocalDateTime inviteExpiresAt;
    InviteStatus inviteStatus;

    public enum InviteStatus {
        VALID, RESENT, EXPIRED, REVOKED, USED
    }

    @JsonIgnore
    public boolean hasPortalUserId() {
        return portalUserId != null;
    }

    @JsonIgnore
    public boolean portalUserIsKnown() {
        return portalUserEmail != null;
    }
}
