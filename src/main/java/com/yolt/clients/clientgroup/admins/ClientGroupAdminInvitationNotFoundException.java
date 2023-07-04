package com.yolt.clients.clientgroup.admins;

import java.util.UUID;

public class ClientGroupAdminInvitationNotFoundException extends RuntimeException {
    public ClientGroupAdminInvitationNotFoundException(UUID clientGroupId, UUID invitationId) {
        super("Could not find invitation %s for client group %s".formatted(invitationId, clientGroupId));
    }
}
