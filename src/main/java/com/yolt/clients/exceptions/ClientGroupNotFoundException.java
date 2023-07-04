package com.yolt.clients.exceptions;

import java.util.UUID;

public class ClientGroupNotFoundException extends IllegalArgumentException {
    public ClientGroupNotFoundException(UUID clientGroupId) {
        super(String.format("No client group found for client group id %s", clientGroupId));
    }
}
