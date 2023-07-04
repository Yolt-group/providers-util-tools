package com.yolt.clients.client.requesttokenpublickeys.exception;

import java.util.UUID;

public class RequestTokenPublicKeyAlreadyStoredException extends RuntimeException{

    public RequestTokenPublicKeyAlreadyStoredException(UUID clientId, String keyId) {
        super("Request token public key is already stored for clientId " + clientId + " and keyId " + keyId);
    }
}
