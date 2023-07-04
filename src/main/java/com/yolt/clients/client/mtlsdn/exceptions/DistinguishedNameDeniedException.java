package com.yolt.clients.client.mtlsdn.exceptions;

import com.yolt.clients.client.mtlsdn.respository.ClientMTLSCertificateDN;

public class DistinguishedNameDeniedException extends RuntimeException {
    public DistinguishedNameDeniedException(ClientMTLSCertificateDN clientMTLSCertificateDN) {
        super("Wrong state (%s), did not %s DN: %s".formatted(clientMTLSCertificateDN.getStatus(), "ADD", clientMTLSCertificateDN.getSubjectDN()));
    }
}
