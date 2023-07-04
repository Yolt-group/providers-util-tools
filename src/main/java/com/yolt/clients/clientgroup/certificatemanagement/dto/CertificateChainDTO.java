package com.yolt.clients.clientgroup.certificatemanagement.dto;

import lombok.Value;
import nl.ing.lovebird.validation.bc.PEM;

@Value
public class CertificateChainDTO {
    @PEM(base64Encoded = true, minObjects = 1, maxObjects = 256)
    String certificateChain;
}
