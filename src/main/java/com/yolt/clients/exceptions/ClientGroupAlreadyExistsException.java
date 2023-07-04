package com.yolt.clients.exceptions;

import java.util.UUID;

public class ClientGroupAlreadyExistsException extends RuntimeException {
    public ClientGroupAlreadyExistsException(UUID newClientGroupId) {
        super("client group with client group id " + newClientGroupId + " already exists in our system");
    }
}
