package com.yolt.clients.exceptions;

public class CannotDeleteRedirectUrlException extends RuntimeException {
    public CannotDeleteRedirectUrlException(RuntimeException e) {
        super(e);
    }
}
