package com.yolt.clients.clientsite.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.springframework.lang.Nullable;

@Value
@NonFinal
@AllArgsConstructor
@Schema(description = "Describes which fields are available and/or required when making a single- or scheduled payment at the site.")
public class PaymentDetailsDTO {

    @Schema(required = true, description = "This field is used to show if the bank supports this payment type.")
    boolean supported;

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    DynamicFieldsDTO dynamicFields;
}
