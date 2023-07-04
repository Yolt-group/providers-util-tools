package com.yolt.clients.exceptions;

import java.util.UUID;

public class InvalidDomainException extends RuntimeException {

    public InvalidDomainException(String domain, UUID clientGroupId) {
        super("The domain " + domain + " is not in the allowed list of domains for client group id " + clientGroupId);
    }
}
