package com.yolt.clients.clientsite.dto;

import com.yolt.clients.sites.pis.Frequency;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;
import org.springframework.lang.Nullable;

import java.util.List;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Describes which fields are available and/or required when making a periodic payment at the site.")
public class PeriodicPaymentDetailsDTO extends PaymentDetailsDTO {

    @NonNull
    @ArraySchema(arraySchema = @Schema(description = "The supported frequencies by the site when initiating a periodic payment."))
    List<Frequency> supportedFrequencies;

    public PeriodicPaymentDetailsDTO(final boolean supported, @Nullable final DynamicFieldsDTO dynamicFields,
                                     @NonNull final List<Frequency> supportedFrequencies) {
        super(supported, dynamicFields);
        this.supportedFrequencies = supportedFrequencies;
    }

}
