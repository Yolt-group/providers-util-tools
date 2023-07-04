package com.yolt.clients.clientgroup.certificatemanagement.eidas;

import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateSigningRequestDTO;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Set;

public class EIDASRequestValidator implements ConstraintValidator<ValidEidasCertificateRequest, CertificateSigningRequestDTO> {
    private static final Set<String> keyAlgorithms = Set.of("RSA2048", "RSA4096");
    private static final Set<String> signatureAlgorithms = Set.of("SHA256_WITH_RSA");

    @Override
    public void initialize(ValidEidasCertificateRequest constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(CertificateSigningRequestDTO input, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (input.getUsageType() == CertificateUsageType.TRANSPORT &&
                (input.getSubjectAlternativeNames() == null || input.getSubjectAlternativeNames().isEmpty())) {
            context.buildConstraintViolationWithTemplate("NO SANS provided for Transport key")
                    .addPropertyNode("subjectAlternativeNames")
                    .addConstraintViolation();
            return false;
        }

        if (input.getKeyAlgorithm() == null || !keyAlgorithms.contains(input.getKeyAlgorithm())) {
            context.buildConstraintViolationWithTemplate("The provided key algorithm is not allowed, must be in " + String.join(",", keyAlgorithms))
                    .addPropertyNode("keyAlgorithm")
                    .addConstraintViolation();
            return false;
        }

        if (input.getSignatureAlgorithm() == null || !signatureAlgorithms.contains(input.getSignatureAlgorithm())) {
            context.buildConstraintViolationWithTemplate("The provided signature algorithm is not allowed, must be in " + String.join(",", signatureAlgorithms))
                    .addPropertyNode("signatureAlgorithm")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
