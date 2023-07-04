package com.yolt.clients.clientgroup.admins;

import java.util.UUID;

public class ClientGroupAdminInviteUsedException extends RuntimeException {
    public ClientGroupAdminInviteUsedException(UUID clientGroupId, UUID invitationId) {
        super("the client admin invite %s for client group %s is used and the portal user is still linked".formatted(invitationId, clientGroupId));
    }
}
