package com.yolt.clients.exceptions;

import java.util.UUID;

public class ClientAlreadyExistsException extends RuntimeException {
    public ClientAlreadyExistsException(UUID newClientId) {
        super("client with client id " + newClientId + " already exists in our system");
    }
}
