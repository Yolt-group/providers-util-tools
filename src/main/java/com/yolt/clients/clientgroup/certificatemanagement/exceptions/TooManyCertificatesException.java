package com.yolt.clients.clientgroup.certificatemanagement.exceptions;

import java.util.UUID;

public class TooManyCertificatesException extends RuntimeException {

    public TooManyCertificatesException(UUID clientGroupId) {
        super(String.format("The limit of the number of certificates for client group with id %s is already reached.", clientGroupId));
    }
}
