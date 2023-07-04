package com.yolt.clients.client.mtlscertificates;

import java.util.UUID;

public class MTLSCertificateExistsException extends RuntimeException {
    public MTLSCertificateExistsException(UUID clientId, String fingerprint) {
        super("A certificate for clientID %s and with fingerprint %s already exists".formatted(clientId, fingerprint));
    }
}
