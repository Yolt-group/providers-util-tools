package com.yolt.clients.clientgroup.certificatemanagement.exceptions;

public class CertificateNotFoundException extends RuntimeException {
    public CertificateNotFoundException(String message) {
        super(message);
    }
}
