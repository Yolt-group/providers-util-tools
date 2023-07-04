package com.yolt.clients.client.admins;

import java.util.UUID;

public class TooManyClientAdminInvitesException extends RuntimeException {
    public TooManyClientAdminInvitesException(UUID clientId, int maxInvites) {
        super("There are too many (more than %d) invites for client %s".formatted(maxInvites, clientId));
    }
}
