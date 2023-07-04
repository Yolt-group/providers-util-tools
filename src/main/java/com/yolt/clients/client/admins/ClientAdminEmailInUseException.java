package com.yolt.clients.client.admins;

import java.util.UUID;

public class ClientAdminEmailInUseException extends RuntimeException {
    public ClientAdminEmailInUseException(UUID clientId, String email, String reason) {
        super("could not invite %s for client %s, reason: %s".formatted(email, clientId, reason));
    }
}
