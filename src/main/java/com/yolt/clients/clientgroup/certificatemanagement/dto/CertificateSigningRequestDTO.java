package com.yolt.clients.clientgroup.certificatemanagement.dto;

import com.yolt.clients.clientgroup.certificatemanagement.validators.In;
import lombok.Data;
import nl.ing.lovebird.providerdomain.ServiceType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Set;

@Data
public class CertificateSigningRequestDTO {
    @NotBlank
    @Size(min = 1, max = 128)
    @Pattern(regexp = "^[\\w\\- ]+$")
    private final String name;

    @NotNull
    private final Set<ServiceType> serviceTypes;

    @NotNull
    private final CertificateUsageType usageType;

    @NotBlank
    @In(values = {"RSA2048", "RSA4096"})
    private final String keyAlgorithm;

    @NotBlank
    @In(values = {"SHA256_WITH_RSA", "SHA384_WITH_RSA", "SHA512_WITH_RSA"})
    private final String signatureAlgorithm;

    @NotNull
    @Size(min = 1, max = 20)
    private final List<SimpleDistinguishedNameElement> subjectDNs;

    @Size(max = 100)
    private final Set<@Size(min = 1, max = 256) String> subjectAlternativeNames;
}
