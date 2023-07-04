package com.yolt.clients.clientgroup.certificatemanagement.exceptions;

public class CertificateValidationException extends Exception {
    public CertificateValidationException(String message) {
        super(message);
    }

    public CertificateValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
