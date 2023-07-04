package com.yolt.clients.clientgroup.certificatemanagement.crypto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SignatureAlgorithm {
    SHA256_WITH_RSA("SHA256withRSA"),
    SHA384_WITH_RSA("SHA384withRSA"),
    SHA512_WITH_RSA("SHA512withRSA");

    private final String algorithm;
}
