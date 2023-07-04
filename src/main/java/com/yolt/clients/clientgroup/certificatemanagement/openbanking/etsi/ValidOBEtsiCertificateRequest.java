package com.yolt.clients.clientgroup.certificatemanagement.openbanking.etsi;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = OBEtsiRequestValidator.class)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RUNTIME)
public @interface ValidOBEtsiCertificateRequest {
    String message() default "Invalid OB Etsi Certificate Request";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
