package com.yolt.clients.exceptions;

import nl.ing.lovebird.clienttokens.ClientToken;

import java.util.UUID;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(ClientToken clientToken) {
        super("could not find the client with clientGroupId " + clientToken.getClientGroupIdClaim() + " and clientId " + clientToken.getClientIdClaim());
    }

    public ClientNotFoundException(UUID clientId) {
        super("could not find the client with clientId " + clientId);
    }
}
