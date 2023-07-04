package com.yolt.clients.sites;

public class SiteNotFoundException extends RuntimeException {
    public SiteNotFoundException(final String message) {
        super(message);
    }
}
