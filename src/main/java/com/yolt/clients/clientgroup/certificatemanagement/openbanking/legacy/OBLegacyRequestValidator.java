package com.yolt.clients.clientgroup.certificatemanagement.openbanking.legacy;

import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateSigningRequestDTO;
import com.yolt.clients.clientgroup.certificatemanagement.dto.SimpleDistinguishedNameElement;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OBLegacyRequestValidator implements ConstraintValidator<ValidOBLegacyCertificateRequest, CertificateSigningRequestDTO> {
    private static final String COUNTRY = "C";
    public static final String ORGANIZATION = "O";
    private static final Set<String> ALLOWED_LEGACY_DNS = Set.of(COUNTRY, "CN", ORGANIZATION, "OU");
    private static final String SIGNATURE_ALGORITHM = "SHA256_WITH_RSA";
    private static final String SUBJECT_DNS = "subjectDNs";

    @Override
    public void initialize(ValidOBLegacyCertificateRequest constraintAnnotation) {
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
        if (value.getServiceTypes() == null || !value.getServiceTypes().isEmpty()) {
            context.buildConstraintViolationWithTemplate("service types list should be empty")
                    .addPropertyNode("serviceTypes")
                    .addConstraintViolation();
            return false;
        }
        Map<String, String> subjectDNs = Optional.ofNullable(value.getSubjectDNs())
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(SimpleDistinguishedNameElement::getType, SimpleDistinguishedNameElement::getValue));
        if (!subjectDNs.keySet().equals(ALLOWED_LEGACY_DNS)) {
            context.buildConstraintViolationWithTemplate("expected only the types: " + String.join(",", ALLOWED_LEGACY_DNS))
                    .addPropertyNode(SUBJECT_DNS)
                    .addConstraintViolation();
            return false;
        }
        if (!"GB".equals(subjectDNs.get(COUNTRY))) {
            context.buildConstraintViolationWithTemplate("the country field in subjectDNs needs to be GB")
                    .addPropertyNode(SUBJECT_DNS)
                    .addConstraintViolation();
            return false;
        }
        if (!"OpenBanking".equals(subjectDNs.get(ORGANIZATION))) {
            context.buildConstraintViolationWithTemplate("the organisation field in subjectDNs needs to be OpenBanking")
                    .addPropertyNode(SUBJECT_DNS)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
