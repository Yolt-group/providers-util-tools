package com.yolt.clients.client.requesttokenpublickeys.dto;

import lombok.Value;
import nl.ing.lovebird.validation.bc.PEM;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

@Value
public class AddRequestTokenPublicKeyDTO {

    @PEM(base64Encoded = true, expectedTypes = SubjectPublicKeyInfo.class)
    String requestTokenPublicKey;
}
