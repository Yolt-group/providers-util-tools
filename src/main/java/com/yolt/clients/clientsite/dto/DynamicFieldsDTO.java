package com.yolt.clients.clientsite.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import org.springframework.lang.Nullable;

@Value
@Builder
@Schema(name = "DynamicFieldsDTO", description = "Describes the types of dynamic fields that are available for the site. Also shows whether the field is required when initiating a payment.")
public class DynamicFieldsDTO {

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    BicDynamicFieldDTO creditorAgentBic;
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    CreditorAgentNameDynamicFieldDTO creditorAgentName;
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    RemittanceInformationStructuredDynamicFieldDTO remittanceInformationStructured;
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    CreditorPostalAddressLineDynamicFieldDTO creditorPostalAddressLine;
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    CreditorPostalCountryDynamicFieldDTO creditorPostalCountry;
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    DebtorNameDynamicFieldDTO debtorName;

    @Value
    @Schema(name = "BicDynamicFieldDTO", description = "Describes whether the BIC can be submitted when initiating a payment for this site.")
    public static class BicDynamicFieldDTO {
        boolean required;
    }

    @Value
    @Schema(name = "CreditorAgentNameDynamicFieldDTO", description = "Describes whether the Organization Name can be submitted when initiating a payment for this site.")
    public static class CreditorAgentNameDynamicFieldDTO {
        boolean required;
    }

    @Value
    @Schema(name = "RemittanceInformationStructuredDynamicFieldDTO", description = "Describes whether the site has the option to submit structured remittance information when initiating a payment.")
    public static class RemittanceInformationStructuredDynamicFieldDTO {
        boolean required;
    }

    @Value
    @Schema(name = "CreditorPostalAddressLineDynamicFieldDTO", description = "Describes whether the creditor postal address can be submitted when initiating a payment for this site.")
    public static class CreditorPostalAddressLineDynamicFieldDTO {
        boolean required;
    }

    @Value
    @Schema(name = "CreditorPostalCountryDynamicFieldDTO", description = "Describes whether the creditor postal country can be submitted when initiating a payment for this site.")
    public static class CreditorPostalCountryDynamicFieldDTO {
        boolean required;
    }

    @Value
    @Schema(name = "DebtorNameDynamicFieldDTO", description = "Describes whether the debtor name can be submitted when initiating a payment for this site.")
    public static class DebtorNameDynamicFieldDTO {
        boolean required;
    }
}
