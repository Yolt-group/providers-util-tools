package com.yolt.clients.client.admins;

import com.yolt.clients.client.admins.models.ClientAdminDTO;
import com.yolt.clients.client.admins.models.ClientAdminInvitation;

import java.util.UUID;

public class ClientAdminInviteInInvalidStateException extends RuntimeException {
    public ClientAdminInviteInInvalidStateException(ClientAdminInvitation invitation, ClientAdminDTO.InviteStatus status, UUID portalUserId) {
        super("Could not link user %s with client %s using invite %s, invite in wrong state %s".formatted(portalUserId, invitation.getClientId(), invitation.getId(), status));
    }

    public ClientAdminInviteInInvalidStateException(ClientAdminInvitation invitation, UUID portalUserId) {
        super("Could not link user %s with client %s using invite %s, the supplied code does not belong to the last invite".formatted(portalUserId, invitation.getClientId(), invitation.getId()));
    }
}
