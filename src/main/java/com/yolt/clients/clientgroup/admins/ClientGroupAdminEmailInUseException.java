package com.yolt.clients.clientgroup.admins;

import java.util.UUID;

public class ClientGroupAdminEmailInUseException extends RuntimeException {
    public ClientGroupAdminEmailInUseException(UUID clientGroupId, String email, String reason) {
        super("could not invite %s for client group %s, reason: %s".formatted(email, clientGroupId, reason));
    }
}
