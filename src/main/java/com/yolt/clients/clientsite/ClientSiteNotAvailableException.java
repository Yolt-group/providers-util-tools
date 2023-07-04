package com.yolt.clients.clientsite;

import com.yolt.clients.sites.Site;

import java.util.UUID;

public class ClientSiteNotAvailableException extends RuntimeException {

    public ClientSiteNotAvailableException(final String message) {
        super(message);
    }

    public ClientSiteNotAvailableException(UUID clientId, Site.SiteId siteId) {
        super("No enabled client site for client '" + clientId + "' and site '" + siteId + "'.");
    }
}
