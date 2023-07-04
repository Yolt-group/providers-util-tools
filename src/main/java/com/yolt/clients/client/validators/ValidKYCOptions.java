package com.yolt.clients.client.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to validate an ISO 3166-1 alpha-2 Country Code.
 */
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidKYCOptionsValidator.class)
@Documented
public @interface ValidKYCOptions {
    String message() default "If KYC is enabled, kycCountryCode must be non-null.  If KYC is not enabled, kycCountryCode must be null.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    boolean optional() default false;
}