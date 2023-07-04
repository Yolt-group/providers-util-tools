package com.yolt.clients.admins.portalusersservice;

import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.ClientToken;

public class PortalUsersFetchException extends RuntimeException {
    public PortalUsersFetchException(ClientGroupToken clientGroupToken) {
        super("Could not fetch the client admins for client group %s".formatted(clientGroupToken.getClientGroupIdClaim()));
    }

    public PortalUsersFetchException(ClientGroupToken clientGroupToken, Throwable cause) {
        super("Could not fetch the client admins for client group %s".formatted(clientGroupToken.getClientGroupIdClaim()), cause);
    }

    public PortalUsersFetchException(ClientToken clientToken) {
        super("Could not fetch the client admins for client %s".formatted(clientToken.getClientGroupIdClaim()));
    }

    public PortalUsersFetchException(ClientToken clientToken, Throwable cause) {
        super("Could not fetch the client admins for client %s".formatted(clientToken.getClientGroupIdClaim()), cause);
    }
}
