package com.yolt.clients.clientgroup.certificatemanagement.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;

public class InConstraintValidator implements ConstraintValidator<In, String> {
    private String[] values;

    @Override
    public void initialize(In constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        values = constraintAnnotation.values();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || Arrays.asList(values).contains(value);
    }
}
