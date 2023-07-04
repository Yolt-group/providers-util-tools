package com.yolt.clients.client.redirecturls.exceptions;

public class RedirectURLMalformedException extends RuntimeException {

    public RedirectURLMalformedException(String redirectURL, Throwable t) {
        super("Redirect URL %s has wrong format.".formatted(redirectURL), t);
    }
}