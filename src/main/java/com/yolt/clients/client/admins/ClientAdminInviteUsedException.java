package com.yolt.clients.client.admins;

import java.util.UUID;

public class ClientAdminInviteUsedException extends RuntimeException {
    public ClientAdminInviteUsedException(UUID clientId, UUID invitationId) {
        super("the client admin invite %s for client %s is used and the portal user is still linked".formatted(invitationId, clientId));
    }
}
