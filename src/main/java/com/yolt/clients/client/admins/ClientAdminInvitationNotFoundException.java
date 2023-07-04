package com.yolt.clients.client.admins;

import java.util.UUID;

public class ClientAdminInvitationNotFoundException extends RuntimeException {
    public ClientAdminInvitationNotFoundException(UUID clientId, UUID invitationId) {
        super("Could not find invitation %s for client %s".formatted(invitationId, clientId));
    }
}
