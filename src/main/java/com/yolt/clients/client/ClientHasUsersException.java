package com.yolt.clients.client;

import java.util.UUID;

public class ClientHasUsersException extends RuntimeException {
    public ClientHasUsersException(UUID clientId, long usersCount) {
        super("Client %s still has %s users".formatted(clientId, usersCount));
    }
}
