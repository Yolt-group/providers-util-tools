package com.yolt.clients.clientgroup.admins;

import java.util.UUID;

public class PortalUserIsClientGroupAdminException extends RuntimeException {
    public PortalUserIsClientGroupAdminException(UUID portalUserId, UUID clientGroupId) {
        super("The portal user %s is already an admin for client group %s".formatted(portalUserId, clientGroupId));
    }
}
