package com.yolt.clients.clientsite;

public class ProviderNotEnabledException extends RuntimeException {

    public ProviderNotEnabledException(final String message) {
        super(message);
    }
}
