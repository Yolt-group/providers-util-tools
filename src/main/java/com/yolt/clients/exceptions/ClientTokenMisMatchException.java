package com.yolt.clients.exceptions;

import nl.ing.lovebird.clienttokens.ClientToken;

import java.util.UUID;

public class ClientTokenMisMatchException extends RuntimeException {
    public ClientTokenMisMatchException(UUID clientGroupId, UUID clientId, ClientToken clientToken) {
        super("the clientGroupId " + clientGroupId + " or clientId " + clientId + " did not match the clientGroupId " + clientToken.getClientGroupIdClaim() + " or clientId " + clientToken.getClientIdClaim() + " in the token");
    }
}
