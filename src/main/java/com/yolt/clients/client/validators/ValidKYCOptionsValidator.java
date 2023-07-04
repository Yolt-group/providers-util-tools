package com.yolt.clients.client.validators;

import com.yolt.clients.client.dto.NewClientDTO;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Ensure kycCountryCode is present in the DTO iff KYC is enabled.
 */
public class ValidKYCOptionsValidator implements ConstraintValidator<ValidKYCOptions, NewClientDTO> {

    @Override
    public boolean isValid(NewClientDTO value, ConstraintValidatorContext constraintValidatorContext) {
        boolean kycEnabled = value.isKycEntities() || value.isKycPrivateIndividuals();
        boolean haveKycCountryCode = value.getKycCountryCode() != null;

        return kycEnabled == haveKycCountryCode;
    }
}
