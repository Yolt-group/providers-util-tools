package com.yolt.clients.admins.portalusersservice;

import java.util.UUID;

public class PortalUserIsGroupAdminException extends RuntimeException {
    public PortalUserIsGroupAdminException(UUID clientGroupId, UUID portalUserId) {
        super("could not determine if user %s is an admin for client group %s, empty response".formatted(portalUserId, clientGroupId));
    }

    public PortalUserIsGroupAdminException(UUID clientGroupId, UUID portalUserId, Exception cause) {
        super("could not determine if user %s is an admin for client group %s, bad response".formatted(portalUserId, clientGroupId), cause);
    }
}
