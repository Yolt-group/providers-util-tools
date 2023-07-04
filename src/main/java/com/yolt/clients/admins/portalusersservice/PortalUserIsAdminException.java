package com.yolt.clients.admins.portalusersservice;

import java.util.UUID;

public class PortalUserIsAdminException extends RuntimeException {
    public PortalUserIsAdminException(UUID clientId, UUID portalUserId) {
        super("could not determine if user %s is an admin for client %s, empty response".formatted(portalUserId, clientId));
    }

    public PortalUserIsAdminException(UUID clientId, UUID portalUserId, Exception cause) {
        super("could not determine if user %s is an admin for client %s, bad response".formatted(portalUserId, clientId), cause);
    }
}
