package com.yolt.clients.admins.portalusersservice;

import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.ClientToken;

import java.util.UUID;

public class RemoveAuthorizationException extends RuntimeException {

    public RemoveAuthorizationException(ClientGroupToken clientGroupToken, UUID portalUserId, Throwable cause) {
        super("Could not remove client group authorization for client group id %s and portal user %s".formatted(clientGroupToken.getClientGroupIdClaim(), portalUserId), cause);
    }

    public RemoveAuthorizationException(ClientToken clientToken, UUID portalUserId, Throwable cause) {
        super("Could not remove client authorization for client id %s and portal user %s".formatted(clientToken.getClientIdClaim(), portalUserId), cause);
    }
}
