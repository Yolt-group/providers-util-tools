package com.yolt.clients.sites.pis;

import lombok.NonNull;
import lombok.Value;
import org.springframework.lang.Nullable;

import java.util.List;

@Value
public class UkDomesticPeriodicPaymentDetails {
    @NonNull List<Frequency> supportedFrequencies;
    boolean supported;
    @NonNull PaymentType type;
    boolean requiresSubmitStep;
    @Nullable
    DynamicFields dynamicFields;
}
