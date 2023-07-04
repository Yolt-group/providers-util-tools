package com.yolt.clients.clientgroup.admins;

import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminDTO;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitation;

import java.util.UUID;

public class ClientGroupAdminInviteInInvalidStateException extends RuntimeException {
    public ClientGroupAdminInviteInInvalidStateException(ClientGroupAdminInvitation invitation, ClientGroupAdminDTO.InviteStatus status, UUID portalUserId) {
        super("Could not link user %s with client group %s using invite %s, invite in wrong state %s".formatted(portalUserId, invitation.getClientGroupId(), invitation.getId(), status));
    }

    public ClientGroupAdminInviteInInvalidStateException(ClientGroupAdminInvitation invitation, UUID portalUserId) {
        super("Could not link user %s with client group %s using invite %s, the supplied code does not belong to the last invite".formatted(portalUserId, invitation.getClientGroupId(), invitation.getId()));
    }
}
