package com.yolt.clients.client.admins;

import java.util.UUID;

public class PortalUserIsClientAdminException extends RuntimeException {
    public PortalUserIsClientAdminException(UUID portalUserId, UUID clientId) {
        super("The portal user %s is already an admin for client %s".formatted(portalUserId, clientId));
    }
}
