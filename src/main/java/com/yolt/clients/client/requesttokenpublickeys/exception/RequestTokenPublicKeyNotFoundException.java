package com.yolt.clients.client.requesttokenpublickeys.exception;

import java.util.UUID;

public class RequestTokenPublicKeyNotFoundException extends RuntimeException {

    public RequestTokenPublicKeyNotFoundException(UUID clientId, String kid) {
        super("No request token public key found for clientId " + clientId + " and keyId " + kid);
    }
}
