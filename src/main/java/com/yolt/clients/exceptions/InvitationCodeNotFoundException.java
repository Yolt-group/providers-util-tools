package com.yolt.clients.exceptions;

public class InvitationCodeNotFoundException extends RuntimeException {

    public InvitationCodeNotFoundException(String code) {
        super("could not find the activation code %.6s***".formatted(code));
    }
}
