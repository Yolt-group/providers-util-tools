package com.yolt.clients.client.redirecturls.exceptions;

import java.util.UUID;

public class RedirectURLNotFoundException extends RuntimeException {
    public RedirectURLNotFoundException(UUID clientId, UUID redirectURLId) {
        super(String.format("RedirectURL with id %s not found for client %s", redirectURLId, clientId));
    }
}
