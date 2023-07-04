package com.yolt.clients.clientgroup.certificatemanagement.openbanking.etsi;

import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateSigningRequestDTO;
import com.yolt.clients.clientgroup.certificatemanagement.dto.SimpleDistinguishedNameElement;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OBEtsiRequestValidator implements ConstraintValidator<ValidOBEtsiCertificateRequest, CertificateSigningRequestDTO> {
    private static final String COUNTRY = "C";
    public static final String ORGANIZATION = "O";
    private static final Set<String> ALLOWED_ETSI_DNS = Set.of(COUNTRY, "CN", ORGANIZATION, "2.5.4.97");
    private static final String SIGNATURE_ALGORITHM = "SHA256_WITH_RSA";

    @Override
    public void initialize(ValidOBEtsiCertificateRequest constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(CertificateSigningRequestDTO value, ConstraintValidatorContext context) {
        if (!SIGNATURE_ALGORITHM.equals(value.getSignatureAlgorithm())) {
            context.buildConstraintViolationWithTemplate("the signature algorithm needs to be " + SIGNATURE_ALGORITHM)
                    .addPropertyNode("signatureAlgorithm")
                    .addConstraintViolation();
            return false;
        }
        if (value.getServiceTypes() == null || value.getServiceTypes().isEmpty()) {
            context.buildConstraintViolationWithTemplate("at least one service type needs to be provided")
                    .addPropertyNode("serviceTypes")
                    .addConstraintViolation();
            return false;
        }
        Map<String, String> subjectDNs = Optional.ofNullable(value.getSubjectDNs())
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(SimpleDistinguishedNameElement::getType, SimpleDistinguishedNameElement::getValue));
        if (!ALLOWED_ETSI_DNS.equals(subjectDNs.keySet())) {
            context.buildConstraintViolationWithTemplate("expected only the types: " + String.join(",", ALLOWED_ETSI_DNS))
                    .addPropertyNode("subjectDNs")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
