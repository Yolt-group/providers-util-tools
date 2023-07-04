package com.yolt.clients.client.redirecturls.exceptions;

import java.util.UUID;

public class RedirectURLAlreadyExistsException extends RuntimeException {

    public RedirectURLAlreadyExistsException(UUID clientId, UUID redirectURLId) {
        super("RedirectURL with id %s for client %s already exists in our system.".formatted(redirectURLId, clientId));
    }

    public RedirectURLAlreadyExistsException(UUID clientId, String redirectURL) {
        super("RedirectURL %s for client %s already exists in our system.".formatted(redirectURL, clientId));
    }
}
