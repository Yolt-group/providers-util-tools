package com.yolt.clients.exceptions;

import java.util.UUID;

public class DomainNotFoundException extends RuntimeException {
    public DomainNotFoundException(String domain, UUID clientGroupId) {
        super("The domain " + domain + " is not found in the allowed list of domains for client group id " + clientGroupId);
    }
}
