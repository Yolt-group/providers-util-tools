package com.yolt.clients.exceptions;

import java.util.UUID;

public class ClientGroupAdminNotFoundException extends IllegalArgumentException {
    public ClientGroupAdminNotFoundException(UUID portalUserId, UUID clientGroupId) {
        super(String.format("No client group admin with portal user id %s found for client group id %s", portalUserId, clientGroupId));
    }
}
