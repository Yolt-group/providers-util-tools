package com.yolt.clients.client.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Locale;

import static java.util.Locale.IsoCountryCode.PART1_ALPHA2;

/**
 * Validates a value to be an ISO 3166-1 alpha-2 Country Code.
 */
public class ValidCountryCodeValidator implements ConstraintValidator<CountryCode, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null) {
            return true;
        }
        return Locale.getISOCountries(PART1_ALPHA2).contains(value);
    }
}